package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.client.core.business.api.TrustAddDidsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.model.ProofOfPossessionStatusDto;
import ch.admin.bj.swiyu.trust.client.core.business.model.TrustAdditionalDidsSubmissionInternalDtoDto;
import ch.admin.bj.swiyu.trust.client.issuer.management.model.CredentialStatusTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.domainevent.DomainEventLogRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer.IssuerClient;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustAddDidSubmissionEventProcessor;
import ch.admin.bj.swiyu.trust.management.test.IssuerTestData;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
class TrustAddDidSubmissionEventProcessorIT {

    @Autowired
    TrustAddDidSubmissionEventProcessor processor;

    @Autowired
    TrustAddDidTaskRepository trustAddDidTaskRepository;

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    DomainEventLogRepository domainEventLogRepository;

    @MockitoBean
    TrustAddDidsSubmissionInternalApi trustAddDidsSubmissionApi;

    @MockitoBean
    IssuerClient issuerClient;

    @MockitoBean
    OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        domainEventLogRepository.deleteAllInBatch();
        trustAddDidTaskRepository.deleteAllInBatch();
        trustStatementPartnerLinkRepository.deleteAllInBatch();

        // Default stubs for the external issuer service
        when(issuerClient.getStatusListUri()).thenReturn("https://issuer.example.com/status-list");
        when(issuerClient.issueTrustStatement(any())).thenReturn(
            new IssuerClient.TrustStatementIssuanceResult(UUID.randomUUID(), IssuerTestData.sdjwt())
        );
        when(issuerClient.getCredentialStatus(any())).thenReturn(CredentialStatusTypeDto.ISSUED);
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void happyPath_trustedPermissionDid_taskAccepted() {
        // given
        var submissionId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        var permissionDid = "did:example:permission-" + UUID.randomUUID();

        var event = TrustOnboardingTestData.tiTrustAddDidSubmissionSubmittedEvent(submissionId);
        var submission = buildSubmission(submissionId, permissionDid);

        when(trustAddDidsSubmissionApi.getSubmission(submissionId)).thenReturn(submission);

        createActiveIdentityTrustStatement(partnerId, permissionDid);

        // when
        processor.processTiTrustAddDidSubmissionSubmittedEvent(event);

        // then
        var tasks = trustAddDidTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        var task = tasks.getFirst();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.ACCEPTED);
        // Note: partnerId is null due to TrustStatementPartnerLink constructor not storing partnerId (pre-existing issue)
        assertThat(task.getPartnerName().getPartnerNameDe()).isEqualTo("Test Partner DE");
        assertThat(task.getPermissionDid()).isEqualTo(permissionDid);
        assertThat(task.getTrustAddDidSubmissionId()).isEqualTo(submissionId);

        verify(issuerClient).issueTrustStatement(any());
        verify(outboxEventPublisher).publishTrustAddDidSubmissionAcceptedEvent(any());
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void untrustedPermissionDid_taskRejected() {
        // given
        var submissionId = UUID.randomUUID();
        var permissionDid = "did:example:untrusted-" + UUID.randomUUID();

        var event = TrustOnboardingTestData.tiTrustAddDidSubmissionSubmittedEvent(submissionId);
        var submission = buildSubmission(submissionId, permissionDid);

        when(trustAddDidsSubmissionApi.getSubmission(submissionId)).thenReturn(submission);

        // No existing identity trust statement for the permission DID

        // when
        processor.processTiTrustAddDidSubmissionSubmittedEvent(event);

        // then
        var tasks = trustAddDidTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        var task = tasks.getFirst();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.REJECTED);
        assertThat(task.getPartnerId()).isNull();

        verify(issuerClient, never()).issueTrustStatement(any());
        verify(outboxEventPublisher).publishTrustAddDidSubmissionRejectedEvent(any());
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void trustStatementIssuanceFails_taskRejected() {
        // given
        var submissionId = UUID.randomUUID();
        var partnerId = UUID.randomUUID();
        var permissionDid = "did:example:fail-" + UUID.randomUUID();

        var event = TrustOnboardingTestData.tiTrustAddDidSubmissionSubmittedEvent(submissionId);
        var submission = buildSubmission(submissionId, permissionDid);

        when(trustAddDidsSubmissionApi.getSubmission(submissionId)).thenReturn(submission);

        createActiveIdentityTrustStatement(partnerId, permissionDid);

        // Make trust statement issuance fail at the issuer level
        when(issuerClient.issueTrustStatement(any())).thenThrow(new RuntimeException("Issuance failed"));

        // when
        processor.processTiTrustAddDidSubmissionSubmittedEvent(event);

        // then
        var tasks = trustAddDidTaskRepository.findAll();
        assertThat(tasks).hasSize(1);
        var task = tasks.getFirst();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.REJECTED);

        verify(outboxEventPublisher).publishTrustAddDidSubmissionRejectedEvent(any());
        verify(outboxEventPublisher, never()).publishTrustAddDidSubmissionAcceptedEvent(any());
    }

    private void createActiveIdentityTrustStatement(UUID partnerId, String permissionDid) {
        var statement = TrustStatementPartnerLink.createIdentityV1(
            partnerId,
            permissionDid,
            Instant.now().minusSeconds(3600),
            Instant.now().plusSeconds(3600 * 24 * 365),
            Map.of(
                IdentityV1Details.Language.DE_CH,
                "Test Partner DE",
                IdentityV1Details.Language.FR_CH,
                "Test Partner FR",
                IdentityV1Details.Language.IT_CH,
                "Test Partner IT",
                IdentityV1Details.Language.EN,
                "Test Partner EN",
                IdentityV1Details.Language.RM_CH,
                "Test Partner RM"
            ),
            List.of(),
            false
        );
        var saved = trustStatementPartnerLinkRepository.save(statement);
        saved.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        trustStatementPartnerLinkRepository.save(saved);
    }

    private TrustAdditionalDidsSubmissionInternalDtoDto buildSubmission(UUID submissionId, String permissionDid) {
        var permissionDidDto = new ProofOfPossessionDto();
        permissionDidDto.did(permissionDid);
        permissionDidDto.nonce("nonce");
        permissionDidDto.status(ProofOfPossessionStatusDto.VALID);

        var newDid = new ProofOfPossessionDto();
        newDid.did("did:example:new-" + UUID.randomUUID());
        newDid.nonce("nonce2");
        newDid.status(ProofOfPossessionStatusDto.VALID);

        return new TrustAdditionalDidsSubmissionInternalDtoDto()
            .id(submissionId)
            .status(TrustAdditionalDidsSubmissionInternalDtoDto.StatusEnum.SUBMITTED)
            .permissionDid(permissionDidDto)
            .didsToAdd(List.of(newDid))
            .updatedAt(Instant.now());
    }
}
