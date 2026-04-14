package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustAddDidTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.PartnerName;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustAddDidTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.TrustOnboardingTaskStatusDto;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ DataJpaTestConfiguration.class, TrustAddDidTaskService.class, DomainEventService.class })
@ActiveProfiles("test")
class TrustAddDidTaskServiceIT {

    @Autowired
    private TrustAddDidTaskService trustAddDidTaskService;

    @Autowired
    private TrustAddDidTaskRepository trustAddDidTaskRepository;

    @Autowired
    private DomainEventLogRepository domainEventLogRepository;

    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventLogRepository.deleteAllInBatch();
        trustAddDidTaskRepository.deleteAllInBatch();
    }

    @Test
    void createTask() {
        // given
        var partnerId = UUID.randomUUID();
        var partnerName = new PartnerName("DE", "FR", "IT", "EN", "RM");
        var submissionId = UUID.randomUUID();
        var permissionDid = "did:example:permission123";
        var submittedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        // when
        var taskId = trustAddDidTaskService.createTask(
            partnerId,
            partnerName,
            submissionId,
            permissionDid,
            submittedAt,
            "test-user"
        );
        commit();

        // then
        var task = trustAddDidTaskRepository.findById(taskId).orElseThrow();
        assertThat(task.getPartnerId()).isEqualTo(partnerId);
        assertThat(task.getPartnerName().getPartnerNameDe()).isEqualTo("DE");
        assertThat(task.getTrustAddDidSubmissionId()).isEqualTo(submissionId);
        assertThat(task.getPermissionDid()).isEqualTo(permissionDid);
        assertThat(task.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(task.getDueAt()).isEqualTo(submittedAt.plus(30, ChronoUnit.DAYS));
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.OPENED);
    }

    @Test
    void createTask_withNullPartnerId() {
        // given
        var partnerName = new PartnerName("Unknown", "Unknown", "Unknown", "Unknown", "Unknown");
        var submissionId = UUID.randomUUID();
        var permissionDid = "did:example:untrusted";
        var submittedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);

        // when
        var taskId = trustAddDidTaskService.createTask(
            null,
            partnerName,
            submissionId,
            permissionDid,
            submittedAt,
            "test-user"
        );
        commit();

        // then
        var task = trustAddDidTaskRepository.findById(taskId).orElseThrow();
        assertThat(task.getPartnerId()).isNull();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.OPENED);
    }

    @Test
    void getTask() {
        // given
        var saved = trustAddDidTaskRepository.save(trustAddDidTask());

        // when
        var dto = trustAddDidTaskService.getTask(saved.getId());

        // then
        assertThat(dto.id()).isEqualTo(saved.getId());
        assertThat(dto.permissionDid()).isEqualTo(saved.getPermissionDid());
        assertThat(dto.trustAddDidSubmissionId()).isEqualTo(saved.getTrustAddDidSubmissionId());
        assertThat(dto.state()).isEqualTo(TrustOnboardingTaskStatusDto.OPENED);
    }

    @Test
    void getTask_notFound() {
        assertThatThrownBy(() -> trustAddDidTaskService.getTask(UUID.randomUUID())).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void accept() {
        // given
        var saved = trustAddDidTaskRepository.save(trustAddDidTask());

        // when
        trustAddDidTaskService.accept(saved.getId());

        // then
        var task = trustAddDidTaskRepository.findById(saved.getId()).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.ACCEPTED);
        verify(outboxEventPublisher).publishTrustAddDidSubmissionAcceptedEvent(any());
    }

    @Test
    void accept_notFound() {
        assertThatThrownBy(() -> trustAddDidTaskService.accept(UUID.randomUUID())).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void reject() {
        // given
        var saved = trustAddDidTaskRepository.save(trustAddDidTask());

        // when
        trustAddDidTaskService.reject(saved.getId(), RejectReason.UNKNOWN);

        // then
        var task = trustAddDidTaskRepository.findById(saved.getId()).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.REJECTED);
        verify(outboxEventPublisher).publishTrustAddDidSubmissionRejectedEvent(any());
    }

    @Test
    void reject_notFound() {
        assertThatThrownBy(() -> trustAddDidTaskService.reject(UUID.randomUUID(), RejectReason.UNKNOWN)).isInstanceOf(
            ResourceNotFoundException.class
        );
    }
}
