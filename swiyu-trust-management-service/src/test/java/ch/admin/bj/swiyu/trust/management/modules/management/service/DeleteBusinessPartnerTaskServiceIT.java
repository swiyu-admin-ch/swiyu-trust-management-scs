package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import ch.admin.bj.swiyu.trust.management.modules.common.config.OffboardingProperties;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerNotDeletableException;
import ch.admin.bj.swiyu.trust.management.modules.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.CreateDeleteBusinessPartnerTaskRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectDeleteBusinessPartnerTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartner;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.DeleteBusinessPartnerTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskType;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestKafkaConfiguration;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        DataJpaTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        DeleteBusinessPartnerTaskService.class,
        DomainEventService.class,
        MockAuditPublisherTestConfiguration.class,
    }
)
@EnableConfigurationProperties({ OffboardingProperties.class, FeaturesProperties.class })
@ActiveProfiles("test")
class DeleteBusinessPartnerTaskServiceIT {

    /**
     * The database container is shared, so every partner this test creates gets its own id and assertions never count
     * rows globally.
     */
    private static final String REASON = "Partner requested deletion";

    @Autowired
    private DeleteBusinessPartnerTaskService service;

    @Autowired
    private TestRepositories repos;

    @Autowired
    private OffboardingProperties offboardingProperties;

    @Test
    void createTask_whenPartnerIsCleared_thenTaskIsOpenedAndDueDateComputedFromTheConfiguredDelay() {
        var partnerId = syncedPartner(true);

        var taskId = service.createTask(request(partnerId), "tester");

        var task = repos.deleteBusinessPartnerTask.findById(taskId).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPENED);
        assertThat(task.getTaskType()).isEqualTo(TaskType.DELETE_BUSINESS_PARTNER);
        assertThat(task.getPartnerId()).isEqualTo(partnerId);
        assertThat(task.getHardDeleteDueAt()).isCloseTo(
            task.getSubmittedAt().plus(offboardingProperties.hardDeleteDelay()),
            within(1, ChronoUnit.SECONDS)
        );
        // dueAt mirrors it so the generic task list shows a due date; hardDeleteDueAt survives approve/reject.
        assertThat(task.getDueAt()).isEqualTo(task.getHardDeleteDueAt());
        // The name is the single string of the partner projection, not a lookup in the partner identity.
        assertThat(service.getTask(taskId, "tester").partnerName()).isEqualTo("ViewTest Partner");
    }

    @Test
    void createTask_whenCoreBusinessServiceHasNotClearedThePartner_thenRejected() {
        var deletionRequest = request(syncedPartner(false));

        assertThatThrownBy(() -> service.createTask(deletionRequest, "tester"))
            .isInstanceOf(BusinessPartnerNotDeletableException.class)
            .hasFieldOrPropertyWithValue("error", BusinessPartnerDeletionError.PARTNER_NOT_CLEARED_FOR_DELETION)
            .hasMessageContaining("not cleared for deletion");
    }

    @Test
    void createTask_whenPartnerWasNeverSynced_thenRejected() {
        var deletionRequest = request(UUID.randomUUID());

        assertThatThrownBy(() -> service.createTask(deletionRequest, "tester"))
            .isInstanceOf(BusinessPartnerNotDeletableException.class)
            .hasFieldOrPropertyWithValue("error", BusinessPartnerDeletionError.PARTNER_NOT_SYNCED)
            .hasMessageContaining("has not been synced");
    }

    @Test
    void createTask_whenARequestIsAlreadyOpen_thenSecondOneRejected() {
        var deletionRequest = request(syncedPartner(true));
        service.createTask(deletionRequest, "tester");

        assertThatThrownBy(() -> service.createTask(deletionRequest, "tester"))
            .isInstanceOf(BusinessPartnerNotDeletableException.class)
            .hasFieldOrPropertyWithValue("error", BusinessPartnerDeletionError.OPEN_DELETION_REQUEST_EXISTS)
            .hasMessageContaining("already has an open deletion request");
    }

    /**
     * The service check cannot see a concurrent insert, so the guarantee has to hold in the database. Writing the
     * second task directly is the single-threaded stand-in for two editors racing each other.
     */
    @Test
    void secondPendingRequest_whenTheServiceCheckIsBypassed_thenTheDatabaseRejectsIt() {
        var partnerId = syncedPartner(true);
        service.createTask(request(partnerId), "tester");
        var submittedAt = Instant.now();
        var duplicate = new DeleteBusinessPartnerTask(
            partnerId,
            "ViewTest Partner",
            submittedAt,
            submittedAt.plusSeconds(60)
        );

        assertThatThrownBy(() -> repos.deleteBusinessPartnerTask.saveAndFlush(duplicate)).isInstanceOf(
            DataIntegrityViolationException.class
        );
    }

    @Test
    void reject_whenTaskIsOpen_thenStatusIsRejectedAndTaskCanBeRequestedAgain() {
        var partnerId = syncedPartner(true);
        var taskId = service.createTask(request(partnerId), "tester");

        service.reject(taskId, new RejectDeleteBusinessPartnerTaskActionDto("changed my mind"), "tester");

        assertThat(service.getTask(taskId, "tester").state()).isEqualTo(TaskStatusDto.REJECTED);
        // A rejected request must not block a later one.
        assertThat(service.createTask(request(partnerId), "tester")).isNotNull();
    }

    private UUID syncedPartner(boolean hardDeleteAllowed) {
        var partnerId = UUID.randomUUID();
        var partner = BusinessPartner.createFor(partnerId);
        partner.sync(
            "ViewTest Partner",
            "CHE-123.456.789",
            BusinessPartnerType.BUSINESS,
            hardDeleteAllowed,
            Instant.now()
        );
        repos.businessPartner.save(partner);
        return partnerId;
    }

    private CreateDeleteBusinessPartnerTaskRequestDto request(UUID partnerId) {
        return new CreateDeleteBusinessPartnerTaskRequestDto(partnerId, REASON, true);
    }
}
