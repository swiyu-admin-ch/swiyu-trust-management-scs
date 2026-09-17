package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustAddDidsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.ProofOfPossessionStatusDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustAdditionalDidsSubmissionInternalDtoDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustAddDidSubmissionEventProcessor;
import ch.admin.bj.swiyu.trust.management.test.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@Import({ StatusListServiceTestConfiguration.class, MockAuditPublisherTestConfiguration.class })
class TrustAddDidSubmissionEventProcessorIT {

    @Autowired
    TrustAddDidSubmissionEventProcessor processor;

    @MockitoBean
    OutboxEventPublisher outboxEventPublisher;

    @MockitoBean
    private TrustAddDidsSubmissionInternalApi trustAddDidsSubmissionInternalApi;

    @Autowired
    private TestRepositories testRepositories;

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

    @BeforeEach
    void setUp() {
        testRepositories.domainEventLog.deleteAllInBatch();
        testRepositories.trustAddDidTask.deleteAllInBatch();
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    @Transactional
    void happyPath_trustedPermissionDid_taskAccepted() {
        // given
        var submissionId = UUID.randomUUID();
        var bpi = BusinessPartnerIdentityTestData.newDefaultBusinessPartnerIdentity();
        var permissionDid = "did:example:permission-" + UUID.randomUUID();
        bpi.getTrustedIdentifier().add(permissionDid);
        testRepositories.businessPartnerIdentity.save(bpi);
        testRepositories.commit();

        var event = TrustOnboardingTestData.tiTrustAddDidSubmissionSubmittedEvent(submissionId);
        var submission = buildSubmission(submissionId, permissionDid);

        when(trustAddDidsSubmissionInternalApi.getSubmission(submissionId)).thenReturn(submission);

        // when
        processor.processTiTrustAddDidSubmissionSubmittedEvent(event);

        // then
        var tasks = testRepositories.trustAddDidTask.findAll();
        assertThat(tasks).hasSize(1);
        var task = tasks.getFirst();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
        // Note: partnerId is null due to TrustStatementPartnerLink constructor not storing partnerId (pre-existing issue)
        assertThat(task.getPartnerName()).containsEntry("de-CH", "Test Partner");
        assertThat(task.getPermissionDid()).isEqualTo(permissionDid);
        assertThat(task.getTrustAddDidSubmissionId()).isEqualTo(submissionId);

        verify(outboxEventPublisher).publishTrustAddDidSubmissionAcceptedEvent(any());
        verify(outboxEventPublisher).publishBusinessPartnerIdentityUpdatedEvent(any());
    }
}
