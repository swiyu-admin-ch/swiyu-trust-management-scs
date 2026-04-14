package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.*;
import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.*;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.security.test.*;
import ch.admin.bj.swiyu.trust.client.core.business.api.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.*;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.*;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.taskaction.*;
import ch.admin.bj.swiyu.trust.management.test.*;
import java.time.temporal.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.jdbc.*;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.*;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EmbeddedKafka
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(
    {
        TrustOnboardingTaskDomainService.class,
        TrustOnboardingTaskService.class,
        DataJpaTestConfiguration.class,
        DataJpaTestKafkaConfiguration.class,
        TrustStatementService.class,
        DomainEventService.class,
        TrustRegistryService.class,
        TrustStatementPartnerLinkValidator.class,
        OutboxEventPublisher.class,
    }
)
@ActiveProfiles("test")
class TrustOnboardingTaskServiceIT {

    @Autowired
    private TrustOnboardingTaskService trustOnboardingTaskService;

    @Autowired
    private TrustOnboardingTaskRepository trustOnboardingTaskRepository;

    @Autowired
    private DomainEventLogRepository domainEventLogRepository;

    @Autowired
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @MockitoBean
    private TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;

    @BeforeEach
    void setUp() {
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
        var statement = trustStatementPartnerLinkRepository.findAll();
        assertThat(statement).hasSize(2);
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
