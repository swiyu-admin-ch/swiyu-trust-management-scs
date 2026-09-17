package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustAddDidTask;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustAddDidsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionStatusDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustAdditionalDidsSubmissionInternalDtoDto;
import ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditPublisher;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustAddDidTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustAddDidTaskRepository;
import ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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

    @MockitoBean // mocked so we don't need to bootstrap kafka
    private OutboxEventPublisher outboxEventPublisher;

    @MockitoBean // mocked so we don't need to bootstrap kafka
    private AuditPublisher auditPublisher;

    @MockitoBean
    private BusinessPartnerIdentityService businessPartnerIdentityService;

    @MockitoBean
    private TrustAddDidsSubmissionInternalApi trustAddDidsSubmissionInternalApi;

    @NotNull
    private TrustAddDidTask createAddDidTestTask() {
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity();
        var task = trustAddDidTask(bpi);
        var did = "did:test:example:%s".formatted(UUID.randomUUID());
        when(businessPartnerIdentityService.getBusinessPartnerIdentity(task.getPartnerId())).thenReturn(bpi);
        when(businessPartnerIdentityService.getBusinessPartnerIdentityByTrustedIdentifier(did)).thenReturn(
            BusinessPartnerIdentityMapper.toBusinessPartnerIdentityDto(bpi)
        );
        when(trustAddDidsSubmissionInternalApi.getSubmission(task.getTrustAddDidSubmissionId())).thenAnswer(mock -> {
            var ret = new TrustAdditionalDidsSubmissionInternalDtoDto();
            var pop = new ProofOfPossessionDto();
            pop.setDid(did);
            pop.setVerifiedAt(Instant.now());
            pop.setStatus(ProofOfPossessionStatusDto.VALID);
            ret.setDidsToAdd(List.of(pop));
            ret.setStatus(TrustAdditionalDidsSubmissionInternalDtoDto.StatusEnum.SUBMITTED);
            ret.setId(task.getTrustAddDidSubmissionId());
            ret.setPermissionDid(pop);
            return ret;
        });
        var ret = trustAddDidTaskRepository.save(task);
        commit();
        return ret;
    }

    @BeforeEach
    void setUp() {
        domainEventLogRepository.deleteAllInBatch();
        trustAddDidTaskRepository.deleteAllInBatch();
    }

    @Test
    void createTask() {
        // given
        var partnerId = UUID.randomUUID();
        var partnerName = Map.of(
            "default",
            "DE",
            "de-CH",
            "DE",
            "fr-CH",
            "FR",
            "it-CH",
            "IT",
            "en",
            "EN",
            "rm-CH",
            "RM"
        );
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
        assertThat(task.getPartnerName().get("de-CH")).isEqualTo("DE");
        assertThat(task.getTrustAddDidSubmissionId()).isEqualTo(submissionId);
        assertThat(task.getPermissionDid()).isEqualTo(permissionDid);
        assertThat(task.getSubmittedAt()).isEqualTo(submittedAt);
        assertThat(task.getDueAt()).isEqualTo(submittedAt.plus(30, ChronoUnit.DAYS));
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPENED);
    }

    @Test
    void createTask_withNullPartnerId() {
        // given
        var partnerName = Map.of("default", "Unknown");
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
        assertThat(task.getStatus()).isEqualTo(TaskStatus.OPENED);
    }

    @Test
    void getTask() {
        // given
        var testTask = createAddDidTestTask();

        // when
        var dto = trustAddDidTaskService.getTask(testTask.getId());

        // then
        assertThat(dto.id()).isEqualTo(testTask.getId());
        assertThat(dto.permissionDid()).isEqualTo(testTask.getPermissionDid());
        assertThat(dto.trustAddDidSubmissionId()).isEqualTo(testTask.getTrustAddDidSubmissionId());
        assertThat(dto.state()).isEqualTo(TaskStatusDto.OPENED);
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
        var testTask = createAddDidTestTask();

        // when
        trustAddDidTaskService.approve(testTask.getId(), "test user");

        // then
        var approvedTask = trustAddDidTaskRepository.findById(testTask.getId()).orElseThrow();
        assertThat(approvedTask.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
        assertThat(approvedTask.getDueAt()).isNull();

        verify(outboxEventPublisher).publishTrustAddDidSubmissionAcceptedEvent(any());
    }

    @Test
    void accept_notFound() {
        assertThatThrownBy(() -> trustAddDidTaskService.approve(UUID.randomUUID(), "test user")).isInstanceOf(
            ResourceNotFoundException.class
        );
    }

    @Test
    void reject() {
        // given
        var testTask = createAddDidTestTask();

        // when
        trustAddDidTaskService.reject(testTask.getId(), RejectReason.UNKNOWN, "test user");

        // then
        var task = trustAddDidTaskRepository.findById(testTask.getId()).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.REJECTED);
        assertThat(task.getDueAt()).isNull();
        verify(outboxEventPublisher).publishTrustAddDidSubmissionRejectedEvent(any());
    }

    @Test
    void reject_notFound() {
        assertThatThrownBy(() ->
            trustAddDidTaskService.reject(UUID.randomUUID(), RejectReason.UNKNOWN, "test user")
        ).isInstanceOf(ResourceNotFoundException.class);
    }
}
