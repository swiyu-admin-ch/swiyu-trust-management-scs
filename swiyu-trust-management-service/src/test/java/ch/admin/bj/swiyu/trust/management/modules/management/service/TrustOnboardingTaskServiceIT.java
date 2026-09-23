package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;
import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustOnboardingSubmissionDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.messaging.avro.security.AvroClassSecurity;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.IdentifierApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustOnboardingRejectReasonDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.TaskFilterDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.ApproveTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RejectTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.RequestMoreInformationTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.task.taskaction.TaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.TrustOnboardingTaskProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.IssuerJwtProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.MockIssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskType;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        AsyncTestConfig.class,
        NonCompliantActorDidsResolver.class,
        BusinessPartnerIdentityDomainService.class,
        BusinessPartnerIdentityService.class,
        DataJpaTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        DomainEventService.class,
        JsonJwtDeserializer.class,
        JwtStatementDomainService.class,
        MockAuditPublisherTestConfiguration.class,
        MockIssuerClient.class,
        OutboxEventPublisher.class,
        StatusListServiceTestConfiguration.class,
        TrustOnboardingTaskService.class,
        TaskService.class,
        DataJpaTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        TrustStatementService.class,
        JsonJwtDeserializer.class,
        DomainEventService.class,
        TrustRegistryService.class,
        TrustStatementPartnerLinkValidator.class,
        TrustStatementService.class,
        BusinessPartnerIdentityEventProcessor.class,
    }
)
@EnableConfigurationProperties(
    {
        IssuerTrustRootProperties.class,
        TrustOnboardingTaskProperties.class,
        IssuerJwtProperties.class,
        IssuerProperties.class,
        DefaultIdentityProperties.class,
    }
)
@ActiveProfiles("test")
class TrustOnboardingTaskServiceIT {

    @Autowired
    AsyncTestConfig asyncTestConfig;

    @MockitoBean
    IdentifierApi identifierApi;

    @Autowired
    private TrustOnboardingTaskService trustOnboardingTaskService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TestRepositories repos;

    @Autowired
    private TrustStatementService trustStatementService;

    @Autowired
    private TrustOnboardingTaskProperties trustOnboardingTaskProperties;

    @MockitoBean
    private TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;

    @MockitoSpyBean
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private BusinessPartnerIdentityEventProcessor businessPartnerIdentityEventProcessor;

    @BeforeAll
    static void installAvroClassWhitelist() {
        AvroClassSecurity.installDefaultIfMissing();
    }

    @BeforeEach
    void setUp() {
        asyncTestConfig.waitForAsyncOperationsFinished();
        repos.domainEventLog.deleteAllInBatch();
        repos.trustStatementPartnerLink.deleteAllInBatch();
        repos.trustOnboardingTask.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void createTaskByTrustOnboardingSubmission() {
        // given
        var submission = trustOnboardingSubmissionDto();

        // when
        this.trustOnboardingTaskService.createTask(submission, getCurrentUserName());
        commit();

        // then
        var task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());

        assertThat(task).isNotNull();
        assertThat(task.getId()).isNotNull();
        assertThat(task.getTrustOnboardingSubmissionId()).isEqualTo(submission.getId());
        assertThat(task.getTaskType()).isEqualTo(TaskType.REGISTRATION);
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
        this.trustOnboardingTaskService.createTask(submission, getCurrentUserName());
        commit();

        var task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());

        // when
        trustOnboardingTaskService.approve(
            task.getId(),
            new ApproveTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );

        // As event handling would be async, it is synchronized here
        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityActivatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityActivatedEvent(captor.capture());
        assertThat(captor.getValue().getPayload().getBusinessPartnerIdentityId()).isEqualTo(submission.getPartnerId());
        businessPartnerIdentityEventProcessor.processTiBusinessPartnerIdentityActivatedEvent(captor.getValue());

        // then
        task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
        assertThat(task.getDueAt()).isNull();

        var statements = repos.trustStatementPartnerLink.findAll();
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
        this.trustOnboardingTaskService.createTask(submission, getCurrentUserName());
        commit();

        var task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());

        // when
        trustOnboardingTaskService.approve(
            task.getId(),
            new ApproveTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );

        // As event handling would be async, it is synchronized here
        var captor = ArgumentCaptor.forClass(TiBusinessPartnerIdentityActivatedEvent.class);
        verify(outboxEventPublisher).publishBusinessPartnerIdentityActivatedEvent(captor.capture());
        assertThat(captor.getValue().getPayload().getBusinessPartnerIdentityId()).isEqualTo(submission.getPartnerId());
        businessPartnerIdentityEventProcessor.processTiBusinessPartnerIdentityActivatedEvent(captor.getValue());

        // then
        task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
        assertThat(task.getDueAt()).isNull();

        var statements = repos.trustStatementPartnerLink.findAll();
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
        this.trustOnboardingTaskService.createTask(submission, getCurrentUserName());
        commit();

        var task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());

        // when
        trustOnboardingTaskService.reject(
            task.getId(),
            new RejectTaskActionDto(TrustOnboardingRejectReasonDto.OTHER, "partner note", "internal note"),
            "Timo Truster"
        );

        // then
        task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.REJECTED);
        assertThat(task.getDueAt()).isNull();
    }

    @Test
    void requestMoreInformation() {
        // given
        var submission = trustOnboardingSubmissionDto();
        this.trustOnboardingTaskService.createTask(submission, getCurrentUserName());
        commit();
        var taskId = repos.trustOnboardingTask
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();

        // when
        trustOnboardingTaskService.requestMoreInformation(
            taskId,
            new RequestMoreInformationTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );

        // then
        var task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.INFORMATION_REQUESTED);
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
        this.trustOnboardingTaskService.createTask(submission, getCurrentUserName());
        commit();
        var taskId = repos.trustOnboardingTask
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();
        var request = new RequestMoreInformationTaskActionDto("partner note", "internal note");

        // 1st round-trip
        trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster");
        trustOnboardingTaskService.createOrResubmitTask(submission, getCurrentUserName());

        // 2nd round-trip
        trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster");
        trustOnboardingTaskService.createOrResubmitTask(submission, getCurrentUserName());

        // when / then - 3rd request is blocked
        assertThrows(IllegalArgumentException.class, () ->
            trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster")
        );
    }

    @Test
    void createOrResubmitTaskByTrustOnboardingSubmission_ExistingTask_MarksResubmitted() {
        // given
        var submission = trustOnboardingSubmissionDto();
        this.trustOnboardingTaskService.createTask(submission, getCurrentUserName());
        commit();
        var taskId = repos.trustOnboardingTask
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();
        trustOnboardingTaskService.requestMoreInformation(
            taskId,
            new RequestMoreInformationTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );

        // when
        var resultId = trustOnboardingTaskService.createOrResubmitTask(submission, getCurrentUserName());

        // then
        assertThat(resultId).isEqualTo(taskId);
        var task = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId());
        assertThat(task.getStatus()).isEqualTo(TaskStatus.RESUBMITTED);
        assertThat(task.getTimesResubmitted()).isEqualTo(1);
        assertThat(task.getRejectionEnforcedAt()).isNull();
        assertThat(task.getDueAt()).isNotNull();
    }

    @Test
    void rejectTasksPastRejectionEnforcementDeadline() {
        // given: a task overdue (past its due date) while OPENED - must NOT be auto-rejected, only reviewers
        // acting on a REQUEST_MORE_INFORMATION deadline trigger auto-rejection
        var overdueOpenSubmission = trustOnboardingSubmissionDto();
        trustOnboardingTaskService.createTask(overdueOpenSubmission, getCurrentUserName());
        commit();
        var overdueOpenTask = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            overdueOpenSubmission.getId()
        );
        overdueOpenTask.overrideDueAt(Instant.now().minus(1, ChronoUnit.DAYS));
        repos.trustOnboardingTask.save(overdueOpenTask);

        // given: a task overdue while INFORMATION_REQUESTED (past its rejection-enforcement deadline)
        var overdueInfoRequestedSubmission = trustOnboardingSubmissionDto();
        trustOnboardingTaskService.createTask(overdueInfoRequestedSubmission, getCurrentUserName());
        var overdueInfoTaskId = repos.trustOnboardingTask
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(overdueInfoRequestedSubmission.getId())
            .getId();
        trustOnboardingTaskService.requestMoreInformation(
            overdueInfoTaskId,
            new RequestMoreInformationTaskActionDto("partner note", "internal note"),
            "Timo Truster"
        );
        var overdueInfoTask = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            overdueInfoRequestedSubmission.getId()
        );
        overdueInfoTask.overrideRejectionEnforcedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        repos.trustOnboardingTask.save(overdueInfoTask);

        // given: a task that is not yet overdue
        var notOverdueSubmission = trustOnboardingSubmissionDto();
        trustOnboardingTaskService.createTask(notOverdueSubmission, getCurrentUserName());
        var notOverdueTask = repos.trustOnboardingTask.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            notOverdueSubmission.getId()
        );
        notOverdueTask.overrideDueAt(Instant.now().plus(30, ChronoUnit.DAYS));
        repos.trustOnboardingTask.save(notOverdueTask);

        // when
        trustOnboardingTaskService.rejectTasksPastRejectionEnforcementDeadline();

        // then
        assertThat(
            repos.trustOnboardingTask
                .getTrustOnboardingTaskByTrustOnboardingSubmissionId(overdueOpenSubmission.getId())
                .getStatus()
        ).isEqualTo(TaskStatus.OPENED);
        assertThat(
            repos.trustOnboardingTask
                .getTrustOnboardingTaskByTrustOnboardingSubmissionId(overdueInfoRequestedSubmission.getId())
                .getStatus()
        ).isEqualTo(TaskStatus.REJECTED);
        assertThat(
            repos.trustOnboardingTask
                .getTrustOnboardingTaskByTrustOnboardingSubmissionId(notOverdueSubmission.getId())
                .getStatus()
        ).isEqualTo(TaskStatus.OPENED);
    }

    @Test
    void getTasks_ResubmissionCapReached_ExcludesRequestMoreInformationAction() {
        // given
        var submission = trustOnboardingSubmissionDto();
        this.trustOnboardingTaskService.createTask(submission, getCurrentUserName());
        commit();
        var taskId = repos.trustOnboardingTask
            .getTrustOnboardingTaskByTrustOnboardingSubmissionId(submission.getId())
            .getId();
        var request = new RequestMoreInformationTaskActionDto("partner note", "internal note");

        // 1st and 2nd round-trip -> resubmission cap reached
        trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster");
        trustOnboardingTaskService.createOrResubmitTask(submission, getCurrentUserName());
        trustOnboardingTaskService.requestMoreInformation(taskId, request, "Timo Truster");
        trustOnboardingTaskService.createOrResubmitTask(submission, getCurrentUserName());
        var filter = new TaskFilterDto(null, null, null, null, null, null, null);
        // when
        var page = taskService.getTasks(PageRequest.of(0, 50), filter, "test user");

        // then
        var item = page
            .getContent()
            .stream()
            .filter(t -> t.id().equals(taskId))
            .findFirst()
            .orElseThrow();
        assertThat(item.allowedActions()).doesNotContain(TaskActionDto.REQUEST_MORE_INFORMATION);
    }
}
