package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.resource.properties.AuthorizationServerConfigProperties;
import ch.admin.bit.jeap.security.resource.properties.ResourceServerProperties;
import ch.admin.bj.swiyu.messagetype.ti.TiHardDeleteBusinessPartnerCommand;
import ch.admin.bj.swiyu.messagetype.ti.common.AuditUser;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.config.OffboardingProperties;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerNotDeletableException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.TaskStatusTransitionException;
import ch.admin.bj.swiyu.trust.management.modules.common.features.FeaturesProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.CreateDeleteBusinessPartnerTaskRequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectDeleteBusinessPartnerTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartner;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLog;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
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
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
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
    private static final String ISSUER = "https://issuer.test/realms/swiyu";

    @Autowired
    private DeleteBusinessPartnerTaskService service;

    @Autowired
    private TestRepositories repos;

    @Autowired
    private OffboardingProperties offboardingProperties;

    @Autowired
    private AuditPublisher auditPublisher;

    @Autowired
    private FeaturesProperties featuresProperties;

    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    @MockitoBean
    private ResourceServerProperties resourceServerProperties;

    @BeforeEach
    void setUp() {
        // The audit publisher is a shared mock bean from MockAuditPublisherTestConfiguration, so invocations of the
        // tests that ran before would otherwise count here.
        clearInvocations(auditPublisher);
        var authorizationServer = new AuthorizationServerConfigProperties();
        authorizationServer.setIssuer(ISSUER);
        when(resourceServerProperties.getAuthorizationServer()).thenReturn(authorizationServer);
    }

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

    @Test
    void approve_whenTaskIsDue_thenTaskIsAcceptedAndTheDeletionIsOrdered() {
        var partnerId = syncedPartner(true);
        var task = dueTask(partnerId);

        service.approve(task.getId(), "SYSTEM");

        var approved = repos.deleteBusinessPartnerTask.findById(task.getId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
        // The generic due date is cleared like on every task, the moment of the scheduled deletion stays readable.
        assertThat(approved.getDueAt()).isNull();
        assertThat(approved.getHardDeleteDueAt()).isEqualTo(task.getHardDeleteDueAt());

        var event = latestEventOfTask(approved);
        assertThat(event.getEventType()).isEqualTo(DomainEventType.BUSINESS_PARTNER_DELETE_ACCEPTED);
        assertThat(event.getTriggeredBy()).isEqualTo("SYSTEM");

        var captor = ArgumentCaptor.forClass(TiHardDeleteBusinessPartnerCommand.class);
        verify(outboxEventPublisher).publishHardDeleteBusinessPartnerCommand(captor.capture());
        var command = captor.getValue();
        assertThat(command.getPayload().getBusinessPartnerId()).isEqualTo(partnerId);
        // The command names the person who requested the deletion, not the job that runs it.
        assertThat(command.getPayload().getTrigger()).isInstanceOf(AuditUser.class);
        var requester = (AuditUser) command.getPayload().getTrigger();
        assertThat(requester.getId()).isEqualTo("TestUser");
        assertThat(requester.getIdentityProvider()).isEqualTo(ISSUER);

        verify(auditPublisher).taskApproved(
            eq(partnerId),
            eq(task.getId()),
            eq(approved.getVersion()),
            eq(TaskType.DELETE_BUSINESS_PARTNER.name()),
            contains("ACCEPTED")
        );
    }

    @Test
    void approve_whenTaskIsNotDueYet_thenNothingHappens() {
        var partnerId = syncedPartner(true);
        var submittedAt = Instant.now();
        var notYetDue = repos.deleteBusinessPartnerTask.saveAndFlush(
            new DeleteBusinessPartnerTask(partnerId, "ViewTest Partner", submittedAt, submittedAt.plusSeconds(3600))
        );

        service.approve(notYetDue.getId(), "SYSTEM");

        assertThat(repos.deleteBusinessPartnerTask.findById(notYetDue.getId()).orElseThrow().getStatus()).isEqualTo(
            TaskStatus.OPENED
        );
        verifyNoInteractions(outboxEventPublisher, auditPublisher);
    }

    @Test
    void approve_whenPartnerIsNoLongerClearedForDeletion_thenTaskStaysOpenAndNothingIsOrdered() {
        var partnerId = syncedPartner(false);
        var task = dueTask(partnerId);

        service.approve(task.getId(), "SYSTEM");

        assertThat(repos.deleteBusinessPartnerTask.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(
            TaskStatus.OPENED
        );
        verifyNoInteractions(outboxEventPublisher, auditPublisher);
    }

    @Test
    void approve_whenPartnerIsNotKnownLocally_thenTaskStaysOpenAndNothingIsOrdered() {
        var task = dueTask(UUID.randomUUID());

        service.approve(task.getId(), "SYSTEM");

        assertThat(repos.deleteBusinessPartnerTask.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(
            TaskStatus.OPENED
        );
        verifyNoInteractions(outboxEventPublisher, auditPublisher);
    }

    @Test
    void approve_whenTaskWasAlreadyApproved_thenRejectedByTheStatusValidator() {
        var partnerId = syncedPartner(true);
        var accepted = new DeleteBusinessPartnerTask(
            partnerId,
            "ViewTest Partner",
            Instant.now().minusSeconds(60),
            Instant.now().minusSeconds(30)
        );
        accepted.approve();
        var task = repos.deleteBusinessPartnerTask.saveAndFlush(accepted);

        assertThatThrownBy(() -> service.approve(task.getId(), "SYSTEM")).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(outboxEventPublisher, auditPublisher);
    }

    @Test
    void approveWithoutDelay_whenTaskIsNotDueYet_thenTaskIsAcceptedAndTheDeletionIsOrdered() {
        var partnerId = syncedPartner(true);
        var taskId = service.createTask(request(partnerId), "tester");
        assertThat(service.getTask(taskId, "tester").state()).isEqualTo(TaskStatusDto.OPENED);

        service.approveWithoutDelay(partnerId, "devops");

        var approved = repos.deleteBusinessPartnerTask.findById(taskId).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
        assertThat(approved.getDueAt()).isNull();
        var event = latestEventOfTask(approved);
        assertThat(event.getEventType()).isEqualTo(DomainEventType.BUSINESS_PARTNER_DELETE_ACCEPTED);
        assertThat(event.getTriggeredBy()).isEqualTo("devops");

        var captor = ArgumentCaptor.forClass(TiHardDeleteBusinessPartnerCommand.class);
        verify(outboxEventPublisher).publishHardDeleteBusinessPartnerCommand(captor.capture());
        assertThat(captor.getValue().getPayload().getBusinessPartnerId()).isEqualTo(partnerId);
        assertThat(((AuditUser) captor.getValue().getPayload().getTrigger()).getId()).isEqualTo("TestUser");

        verify(auditPublisher).taskApproved(
            eq(partnerId),
            eq(taskId),
            eq(approved.getVersion()),
            eq(TaskType.DELETE_BUSINESS_PARTNER.name()),
            contains("ACCEPTED")
        );
    }

    @Test
    void approveWithoutDelay_whenPartnerIsNoLongerClearedForDeletion_thenRejected() {
        var partnerId = syncedPartner(true);
        var taskId = service.createTask(request(partnerId), "tester");
        syncPartner(partnerId, false);

        assertThatThrownBy(() -> service.approveWithoutDelay(partnerId, "devops"))
            .isInstanceOf(BusinessPartnerNotDeletableException.class)
            .hasFieldOrPropertyWithValue("error", BusinessPartnerDeletionError.PARTNER_NOT_CLEARED_FOR_DELETION);

        assertThat(repos.deleteBusinessPartnerTask.findById(taskId).orElseThrow().getStatus()).isEqualTo(
            TaskStatus.OPENED
        );
        verifyNoInteractions(outboxEventPublisher);
        verify(auditPublisher, never()).taskApproved(any(), any(), any(), any(), any());
    }

    @Test
    void approveWithoutDelay_whenThereIsNoDeletionRequest_thenNotFound() {
        assertThatThrownBy(() -> service.approveWithoutDelay(UUID.randomUUID(), "devops")).isInstanceOf(
            ResourceNotFoundException.class
        );
        verifyNoInteractions(outboxEventPublisher);
    }

    @Test
    void approveWithoutDelay_whenTheLatestRequestWasAlreadyApproved_thenConflict() {
        var partnerId = syncedPartner(true);
        service.createTask(request(partnerId), "tester");
        service.approveWithoutDelay(partnerId, "devops");

        assertThatThrownBy(() -> service.approveWithoutDelay(partnerId, "devops"))
            .isInstanceOf(TaskStatusTransitionException.class)
            .isInstanceOf(IllegalStateException.class);
        verify(outboxEventPublisher, times(1)).publishHardDeleteBusinessPartnerCommand(any());
    }

    @Test
    void approveWithoutDelay_whenTheFeatureIsDisabled_thenNotFound() {
        var partnerId = syncedPartner(true);
        service.createTask(request(partnerId), "tester");

        featuresProperties.setEidartfe1571CompleteBpOffboarding(false);
        try {
            assertThatThrownBy(() -> service.approveWithoutDelay(partnerId, "devops")).isInstanceOfSatisfying(
                ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        } finally {
            featuresProperties.setEidartfe1571CompleteBpOffboarding(true);
        }
        verifyNoInteractions(outboxEventPublisher);
    }

    private DeleteBusinessPartnerTask dueTask(UUID partnerId) {
        var submittedAt = Instant.now().minusSeconds(60);
        return repos.deleteBusinessPartnerTask.saveAndFlush(
            new DeleteBusinessPartnerTask(partnerId, "ViewTest Partner", submittedAt, submittedAt.plusSeconds(30))
        );
    }

    private DomainEventLog latestEventOfTask(DeleteBusinessPartnerTask task) {
        return repos.domainEventLog
            .findAll()
            .stream()
            .filter(event -> task.getId().equals(event.getTaskId()))
            .max(Comparator.comparing(DomainEventLog::getTriggeredAt))
            .orElseThrow();
    }

    private void syncPartner(UUID partnerId, boolean hardDeleteAllowed) {
        var partner = repos.businessPartner.findById(partnerId).orElseThrow();
        partner.sync(
            "ViewTest Partner",
            "CHE-123.456.789",
            BusinessPartnerType.BUSINESS,
            hardDeleteAllowed,
            Instant.now()
        );
        repos.businessPartner.saveAndFlush(partner);
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
