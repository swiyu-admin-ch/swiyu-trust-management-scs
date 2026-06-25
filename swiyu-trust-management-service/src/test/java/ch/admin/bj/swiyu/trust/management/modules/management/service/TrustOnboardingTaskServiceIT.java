package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;
import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustOnboardingSubmissionDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.taskaction.ApproveTaskActionDto;
import ch.admin.bj.swiyu.trust.management.test.*;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
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
@EnableConfigurationProperties({ IssuerTrustRootProperties.class })
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
        assertThat(task.getPartnerName().getPartnerNameDe()).isEqualTo(submission.getEntityName().getDe());
        assertThat(task.getPartnerName().getPartnerNameFr()).isEqualTo(submission.getEntityName().getFr());
        assertThat(task.getPartnerName().getPartnerNameIt()).isEqualTo(submission.getEntityName().getIt());
        assertThat(task.getPartnerName().getPartnerNameEn()).isEqualTo(submission.getEntityName().getEn());
        assertThat(task.getPartnerName().getPartnerNameRm()).isEqualTo(submission.getEntityName().getRm());
        Assertions.assertNotNull(submission.getCreatedAt());
        Assertions.assertNotNull(submission.getSubmittedAt());
        assertThat(task.getSubmittedAt()).isEqualTo(submission.getSubmittedAt());
        Assertions.assertNotNull(submission.getUpdatedAt());
        Assertions.assertNotNull(submission.getSubmittedAt());
        assertThat(task.getDueAt()).isEqualTo(submission.getSubmittedAt().plus(30, ChronoUnit.DAYS));
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
}
