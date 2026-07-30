package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;
import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustOnboardingSubmissionDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.TrustOnboardingTaskStatusValidationException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingRejectReasonDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.ApproveTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RejectTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.RequestMoreInformationTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.TrustOnboardingTaskProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EmbeddedKafka
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        JwtStatementDomainService.class,
        TrustOnboardingTaskDomainService.class,
        TrustOnboardingTaskService.class,
        DataJpaTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        TrustStatementService.class,
        JsonJwtDeserializer.class,
        DomainEventService.class,
        TrustRegistryService.class,
        TrustStatementPartnerLinkValidator.class,
        OutboxEventPublisher.class,
        StatusListServiceTestConfiguration.class,
        AsyncTestConfig.class,
        MockAuditPublisherTestConfiguration.class,
    }
)
@EnableConfigurationProperties({ IssuerTrustRootProperties.class, TrustOnboardingTaskProperties.class })
@ActiveProfiles("test")
class TrustOnboardingTaskServiceIT {

    @Autowired
    AsyncTestConfig asyncTestConfig;

    @Autowired
    private TrustOnboardingTaskService trustOnboardingTaskService;

    @Autowired
    private TrustOnboardingTaskRepository trustOnboardingTaskRepository;

    @Autowired
    private DomainEventLogRepository domainEventLogRepository;

    @Autowired
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    private TrustStatementService trustStatementService;

    @Autowired
    private TrustOnboardingTaskProperties trustOnboardingTaskProperties;

    @MockitoBean
    private TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        domainEventLogRepository.deleteAllInBatch();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
        trustOnboardingTaskRepository.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void createTaskByTrustOnboardingSubmission() {
        // given
        var submission = trustOnboardingSubmissionDto();

        // when
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();

        // then
        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            submission.getId()
        );

        assertThat(task).isNotNull();
        assertThat(task.getId()).isNotNull();
        assertThat(task.getTrustOnboardingSubmissionId()).isEqualTo(submission.getId());
        assertThat(task.getPartnerName()).isEqualTo(submission.getName());
        Assertions.assertNotNull(submission.getCreatedAt());
        Assertions.assertNotNull(submission.getSubmittedAt());
        assertThat(task.getSubmittedAt()).isEqualTo(submission.getSubmittedAt());
        Assertions.assertNotNull(submission.getUpdatedAt());
        Assertions.assertNotNull(submission.getSubmittedAt());
        assertThat(task.getDueAt()).isEqualTo(
            submission.getSubmittedAt().plus(trustOnboardingTaskProperties.dueDatePeriod())
        );
    }

    @Test
    void approve() {
        // given
        var submission = trustOnboardingSubmissionDto();
        when(trustOnboardingSubmissionApi.getTrustOnboardingSubmission(submission.getId())).thenReturn(submission);
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();

        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            submission.getId()
        );

        // when
        trustOnboardingTaskService.approve(
            task.getId(),
            new ApproveTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );

        // then
        task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.ACCEPTED);
        assertThat(task.getDueAt()).isNull();

        var statements = trustStatementPartnerLinkRepository.findAll();
        assertThat(statements).hasSize(4);
        assertThat(
            statements.stream().filter(s -> s.getType() == TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1)
        ).hasSize(2);
        assertThat(
            statements.stream().filter(s -> s.getType() == TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2)
        ).hasSize(2);
    }

    @Test
    void approveWithPreviousTrustOnboarding() {
        // given
        var submission = trustOnboardingSubmissionDto();
        trustStatementService.issueAndPublishIdentityV1TrustStatement(
            submission.getPartnerId(),
            RequestTestData.tsIdentityV1RequestDto(submission.getProofOfPossessions().getFirst().getDid())
        );
        trustStatementService.issueAndPublishIdentityV2TrustStatement(
            RequestTestData.tsIdentityV2RequestDto(
                submission.getPartnerId(),
                submission.getProofOfPossessions().getFirst().getDid()
            )
        );
        when(trustOnboardingSubmissionApi.getTrustOnboardingSubmission(submission.getId())).thenReturn(submission);
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();

        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            submission.getId()
        );

        // when
        trustOnboardingTaskService.approve(
            task.getId(),
            new ApproveTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );

        // then
        task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.ACCEPTED);
        assertThat(task.getDueAt()).isNull();

        var statements = trustStatementPartnerLinkRepository.findAll();
        assertThat(statements).hasSize(6);
        assertThat(
            statements.stream().filter(s -> s.getType() == TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1)
        ).hasSize(3);
        assertThat(
            statements.stream().filter(s -> s.getType() == TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2)
        ).hasSize(3);
        assertThat(
            statements
                .stream()
                .filter(
                    s ->
                        s.getType() == TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1 &&
                        s.getStatus() == TrustStatementPartnerLinkStatus.ACTIVE
                )
        ).hasSize(2);
        assertThat(
            statements
                .stream()
                .filter(
                    s ->
                        s.getType() == TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2 &&
                        s.getStatus() == TrustStatementPartnerLinkStatus.ACTIVE
                )
        ).hasSize(2);
    }

    @Test
    void reject() {
        // given
        var submission = trustOnboardingSubmissionDto();
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();

        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            submission.getId()
        );

        // when
        trustOnboardingTaskService.reject(
            task.getId(),
            new RejectTaskActionDto(TrustOnboardingRejectReasonDto.OTHER, "partner note", "internal note"),
            "Timo Truster"
        );

        // then
        task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.REJECTED);
        assertThat(task.getDueAt()).isNull();
    }

    @Test
    void assign() {
        // given
        var submission = trustOnboardingSubmissionDto();
        when(trustOnboardingSubmissionApi.getTrustOnboardingSubmission(submission.getId())).thenReturn(submission);
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();
        var taskId = trustOnboardingTaskRepository
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();

        // when
        trustOnboardingTaskService.assign(taskId, "Timo Truster", "Tina Trusty");

        // then
        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            submission.getId()
        );
        assertThat(task.getAssignee()).isEqualTo("Timo Truster");
    }

    @Test
    void addInternalNote() {
        // given
        var note = "This actor needs some more validation";
        var triggeredBy = "Tina Trusty";
        var submission = trustOnboardingSubmissionDto();
        when(trustOnboardingSubmissionApi.getTrustOnboardingSubmission(submission.getId())).thenReturn(submission);
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();
        var taskId = trustOnboardingTaskRepository
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();

        // when
        trustOnboardingTaskService.addInternalNote(taskId, note, triggeredBy);

        // then
        var event = domainEventLogRepository.findAll(Sort.by(Sort.Order.desc("triggeredAt"))).getFirst();
        assertThat(event.getInternalNote()).isEqualTo(note);
    }

    @Test
    void requestMoreInformation() {
        // given
        var submission = trustOnboardingSubmissionDto();
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();
        var taskId = trustOnboardingTaskRepository
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();

        // when
        trustOnboardingTaskService.requestMoreInformation(
            taskId,
            new RequestMoreInformationTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );

        // then
        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            submission.getId()
        );
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.INFORMATION_REQUESTED);
        assertThat(task.getDueAt()).isNull();
        assertThat(task.getRejectionEnforcedAt()).isCloseTo(
            Instant.now().plus(trustOnboardingTaskProperties.rejectionEnforcementPeriod()),
            within(5, ChronoUnit.SECONDS)
        );
    }

    @Test
    void requestMoreInformation_ResubmissionCapReached_Throws() {
        // given
        var submission = trustOnboardingSubmissionDto();
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();
        var taskId = trustOnboardingTaskRepository
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();
        var request = new RequestMoreInformationTaskActionDto("partner note", "internal note");

        // 1st round-trip
        trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster");
        trustOnboardingTaskService.createOrResubmitTaskByTrustOnboardingSubmission(submission, getCurrentUserName());

        // 2nd round-trip
        trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster");
        trustOnboardingTaskService.createOrResubmitTaskByTrustOnboardingSubmission(submission, getCurrentUserName());

        // when / then - 3rd request is blocked
        assertThrows(TrustOnboardingTaskStatusValidationException.class, () ->
            trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster")
        );
    }

    @Test
    void createOrResubmitTaskByTrustOnboardingSubmission_ExistingTask_MarksResubmitted() {
        // given
        var submission = trustOnboardingSubmissionDto();
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();
        var taskId = trustOnboardingTaskRepository
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();
        trustOnboardingTaskService.requestMoreInformation(
            taskId,
            new RequestMoreInformationTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );

        // when
        var resultId = trustOnboardingTaskService.createOrResubmitTaskByTrustOnboardingSubmission(
            submission,
            getCurrentUserName()
        );

        // then
        assertThat(resultId).isEqualTo(taskId);
        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            submission.getId()
        );
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.RESUBMITTED);
        assertThat(task.getTimesResubmitted()).isEqualTo(1);
        assertThat(task.getRejectionEnforcedAt()).isNull();
        assertThat(task.getDueAt()).isNotNull();
    }

    @Test
    void rejectTasksPastRejectionEnforcementDeadline() {
        // given: a task overdue (past its due date) while OPENED - must NOT be auto-rejected, only reviewers
        // acting on a REQUEST_MORE_INFORMATION deadline trigger auto-rejection
        var overdueOpenSubmission = trustOnboardingSubmissionDto();
        trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(overdueOpenSubmission, getCurrentUserName());
        commit();
        var overdueOpenTask = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            overdueOpenSubmission.getId()
        );
        overdueOpenTask.setDueAt(Instant.now().minus(1, ChronoUnit.DAYS));
        trustOnboardingTaskRepository.save(overdueOpenTask);

        // given: a task overdue while INFORMATION_REQUESTED (past its rejection-enforcement deadline)
        var overdueInfoRequestedSubmission = trustOnboardingSubmissionDto();
        trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(
            overdueInfoRequestedSubmission,
            getCurrentUserName()
        );
        var overdueInfoTaskId = trustOnboardingTaskRepository
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(overdueInfoRequestedSubmission.getId())
            .getId();
        trustOnboardingTaskService.requestMoreInformation(
            overdueInfoTaskId,
            new RequestMoreInformationTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );
        var overdueInfoTask = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            overdueInfoRequestedSubmission.getId()
        );
        overdueInfoTask.overrideRejectionEnforcedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        trustOnboardingTaskRepository.save(overdueInfoTask);

        // given: a task that is not yet overdue
        var notOverdueSubmission = trustOnboardingSubmissionDto();
        trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(notOverdueSubmission, getCurrentUserName());
        var notOverdueTask = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            notOverdueSubmission.getId()
        );
        notOverdueTask.setDueAt(Instant.now().plus(30, ChronoUnit.DAYS));
        trustOnboardingTaskRepository.save(notOverdueTask);

        // when
        trustOnboardingTaskService.rejectTasksPastRejectionEnforcementDeadline();

        // then
        assertThat(
            trustOnboardingTaskRepository
                .getTrustOnboardingTaskByTrustOnboardingSubmissionId(overdueOpenSubmission.getId())
                .getStatus()
        ).isEqualTo(TrustTaskStatus.OPENED);
        assertThat(
            trustOnboardingTaskRepository
                .getTrustOnboardingTaskByTrustOnboardingSubmissionId(overdueInfoRequestedSubmission.getId())
                .getStatus()
        ).isEqualTo(TrustTaskStatus.REJECTED);
        assertThat(
            trustOnboardingTaskRepository
                .getTrustOnboardingTaskByTrustOnboardingSubmissionId(notOverdueSubmission.getId())
                .getStatus()
        ).isEqualTo(TrustTaskStatus.OPENED);
    }

    @Test
    void getTasks_ResubmissionCapReached_ExcludesRequestMoreInformationAction() {
        // given
        var submission = trustOnboardingSubmissionDto();
        this.trustOnboardingTaskService.createTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        commit();
        var taskId = trustOnboardingTaskRepository
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();
        var request = new RequestMoreInformationTaskActionDto("partner note", "internal note");

        // 1st and 2nd round-trip -> resubmission cap reached
        trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster");
        trustOnboardingTaskService.createOrResubmitTaskByTrustOnboardingSubmission(submission, getCurrentUserName());
        trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster");
        trustOnboardingTaskService.createOrResubmitTaskByTrustOnboardingSubmission(submission, getCurrentUserName());

        // when
        var page = trustOnboardingTaskService.getTasks(PageRequest.of(0, 50), null, null, null, null, null, null, null);

        // then
        var item = page
            .getContent()
            .stream()
            .filter(t -> t.id().equals(taskId))
            .findFirst()
            .orElseThrow();
        assertThat(item.allowedActions()).doesNotContain(TrustOnboardingTaskActionDto.REQUEST_MORE_INFORMATION);
    }
}
