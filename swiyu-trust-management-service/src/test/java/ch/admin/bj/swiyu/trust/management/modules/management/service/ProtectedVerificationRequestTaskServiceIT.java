package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.test.TestTransactionSupport.commit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.ProtectedVerificationSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProtectedVerificationCategoryDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProtectedVerificationSubmissionDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProtectedVerificationSubmissionStatusDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.api.UsnApi;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.OrganisationDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.StatusDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.UsnDto;
import ch.admin.bj.swiyu.trust.client.zas.sbn.model.UsnPageDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.taskaction.ApproveProtectedVerificationRequestTaskActionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.config.ProtectedVerificationTaskProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentity;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.JwtStatementDomainService;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationField;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedVerificationRequestTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkValidator;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness.IssuerTrustRootProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.JsonJwtDeserializer;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryService;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.MockAuditPublisherTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.StatusListServiceTestConfiguration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@Import(
    {
        ProtectedVerificationRequestTaskService.class,
        BusinessPartnerIdentityService.class,
        DataJpaTestConfiguration.class,
        TrustStatementService.class,
        JsonJwtDeserializer.class,
        DomainEventService.class,
        TrustRegistryService.class,
        TrustStatementPartnerLinkValidator.class,
        JwtStatementDomainService.class,
        StatusListServiceTestConfiguration.class,
        MockAuditPublisherTestConfiguration.class,
    }
)
@EnableConfigurationProperties(
    { IssuerTrustRootProperties.class, DefaultIdentityProperties.class, ProtectedVerificationTaskProperties.class }
)
@ActiveProfiles("test")
class ProtectedVerificationRequestTaskServiceIT {

    private static final UUID PARTNER_ID = UUID.randomUUID();
    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID SBN_ID = UUID.randomUUID();

    @Autowired
    private ProtectedVerificationRequestTaskService service;

    @Autowired
    private ProtectedVerificationRequestTaskRepository taskRepository;

    @Autowired
    private DomainEventLogRepository domainEventLogRepository;

    @Autowired
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    private BusinessPartnerIdentityRepository businessPartnerIdentityRepository;

    @Autowired
    private ProtectedVerificationRepository protectedVerificationRepository;

    @Autowired
    private StatementRepository statementRepository;

    @MockitoBean
    private ProtectedVerificationSubmissionInternalApi protectedVerificationSubmissionApi;

    @MockitoBean
    private UsnApi usnApi;

    @MockitoBean
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventLogRepository.deleteAllInBatch();
        statementRepository.deleteAllInBatch();
        protectedVerificationRepository.deleteAllInBatch();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
        businessPartnerIdentityRepository.deleteAllInBatch();
        taskRepository.deleteAllInBatch();
    }

    @Test
    void createTask_trustedPartner_createsTaskAndPublishesDomainEvent() {
        // given
        var submission = submissionDto();
        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );
        seedBusinessPartnerIdentity();

        // when
        var taskId = service.createTask(SUBMISSION_ID, "tester");
        commit();

        // then
        var task = taskRepository.findById(taskId).orElseThrow();
        assertThat(task.getPartnerId()).isEqualTo(PARTNER_ID);
        assertThat(task.getProtectedVerificationSubmissionId()).isEqualTo(SUBMISSION_ID);
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.OPENED);

        var event = domainEventLogRepository
            .findAll()
            .stream()
            .filter(e -> taskId.equals(e.getTrustTaskId()))
            .findFirst()
            .orElseThrow();
        assertThat(event.getEventType()).isEqualTo(DomainEventType.PROTECTED_VERIFICATION_REQUEST_RECEIVED);
    }

    @Test
    void approve_addsProtectedVerificationAuthorizationAndPublishesEvents() {
        // given
        var submission = submissionDto();
        stubExternalClients(submission);
        seedBusinessPartnerIdentity();
        var taskId = service.createTask(SUBMISSION_ID, "tester");
        commit();
        service.getZasData(taskId);
        service.markZasDataReviewed(taskId);

        // when
        service.approve(taskId, new ApproveProtectedVerificationRequestTaskActionDto("internal note"), "Timo Truster");

        // then
        var task = taskRepository.findById(taskId).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.ACCEPTED);

        // The authorization is persisted synchronously; actual trust statement issuance for the partner's trusted
        // DIDs happens asynchronously afterward, triggered by the BusinessPartnerIdentity-updated event below -
        // that hand-off (and Registry DB persistence) is covered by BusinessPartnerIdentityServiceIT, not here.
        var authorizations =
            protectedVerificationRepository.findAllByBusinessPartnerIdentityIdAndProtectedVerificationField(
                PARTNER_ID,
                ProtectedVerificationField.AHV_NUMBER
            );
        assertThat(authorizations).hasSize(1);

        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(any());
        verify(outboxEventPublisher).publishProtectedVerificationSubmissionApprovedEvent(any());

        var approvedEvent = domainEventLogRepository
            .findAll()
            .stream()
            .filter(
                e ->
                    taskId.equals(e.getTrustTaskId()) &&
                    e.getEventType() == DomainEventType.PROTECTED_VERIFICATION_REQUEST_APPROVED
            )
            .findFirst()
            .orElseThrow();
        assertThat(approvedEvent.getInternalNote()).isEqualTo("internal note");
    }

    @Test
    void createTask_untrustedPartner_autoRejects() {
        // given: no BusinessPartnerIdentity seeded, so the partner is not (yet) trusted
        var submission = submissionDto();
        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );

        // when
        var taskId = service.createTask(SUBMISSION_ID, "tester");
        commit();

        // then
        var task = taskRepository.findById(taskId).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.REJECTED);

        verify(outboxEventPublisher).publishProtectedVerificationSubmissionRejectedEvent(any());

        var rejectedEvent = domainEventLogRepository
            .findAll()
            .stream()
            .filter(
                e ->
                    taskId.equals(e.getTrustTaskId()) &&
                    e.getEventType() == DomainEventType.PROTECTED_VERIFICATION_REQUEST_REJECTED
            )
            .findFirst()
            .orElseThrow();
        assertThat(rejectedEvent.getPartnerNote()).contains("not yet onboarded to the trust registry");
    }

    private ProtectedVerificationSubmissionDto submissionDto() {
        return new ProtectedVerificationSubmissionDto()
            .id(SUBMISSION_ID)
            .partnerId(PARTNER_ID)
            .sbnId(SBN_ID)
            .entityName("Acme AG")
            .category(ProtectedVerificationCategoryDto.PERSONAL_ADMINISTRATIVE_NUMBER)
            .reason("test reason")
            .status(ProtectedVerificationSubmissionStatusDto.SUBMITTED)
            .submittedAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
            .createdAt(Instant.now().truncatedTo(ChronoUnit.MICROS))
            .updatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
    }

    private UsnPageDto usnPageWith(UsnDto usn) {
        return new UsnPageDto().content(List.of(usn));
    }

    private UsnDto usnDto() {
        return new UsnDto()
            .businessId(SBN_ID)
            .organisation(new OrganisationDto().name("Acme AG").locality("Bern"))
            .status(new StatusDto().code("ACTIVE"));
    }

    private void stubExternalClients(ProtectedVerificationSubmissionDto submission) {
        when(protectedVerificationSubmissionApi.getProtectedVerificationSubmission(SUBMISSION_ID)).thenReturn(
            submission
        );
        when(
            usnApi.searchUsns(
                eq(SBN_ID),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).thenReturn(usnPageWith(usnDto()));
    }

    /**
     * Seeds a {@link BusinessPartnerIdentity} row for the partner, which is what {@code createTask} checks for
     * (existence only, regardless of status) to decide whether to auto-reject.
     */
    private void seedBusinessPartnerIdentity() {
        businessPartnerIdentityRepository.save(
            new BusinessPartnerIdentity(
                PARTNER_ID,
                Map.of("default", "Acme AG"),
                Instant.now(),
                "CHE-123.456.789",
                true,
                "de-CH",
                BusinessPartnerIdentityStatus.ACTIVE,
                false,
                Instant.now().plusSeconds(3600),
                null,
                Set.of()
            )
        );
    }
}
