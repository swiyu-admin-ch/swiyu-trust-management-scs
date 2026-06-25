package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.consumer;

import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustOnboardingSubmissionDto;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.messaging.kafka.interceptor.JeapKafkaMessageCallback;
import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSucceededEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustOnboardingTask;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustOnboardingTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingSubmissionEventProcessor;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@TestPropertySource(properties = "app.functionality.automaticApprovalEnabled=true")
class TrustOnboardingSubmissionEventProcessorAutomaticApprovalEnabledIT {

    @Autowired
    TrustOnboardingSubmissionEventProcessor trustOnboardingSubmissionEventProcessor;

    @Autowired
    TrustOnboardingTaskRepository trustOnboardingTaskRepository;

    @MockitoBean
    TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;

    @MockitoBean
    TrustStatementService trustStatementService;

    @MockitoBean // registers a callback so we can verify the sent message
    JeapKafkaMessageCallback kafkaMsgCallback;

    private void verifyTiTrustOnboardingSucceededEventWasSend(UUID submissionId) {
        var messageCaptor = ArgumentCaptor.forClass(TiTrustOnboardingSucceededEvent.class);
        verify(kafkaMsgCallback, times(1)).onSend(messageCaptor.capture(), any());
        var msg = messageCaptor.getValue();
        Assertions.assertThat(msg.getOptionalPayload().orElseThrow().getTrustOnboardingSubmissionId()).isEqualTo(
            submissionId
        );
    }

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void whenAutomaticApprovalEnabled_taskIsApprovedImmediately() {
        // given
        var event = TrustOnboardingTestData.tiTrustOnboardingSubmissionAcceptedEvent();
        var trustOnboardingSubmission = trustOnboardingSubmissionDto(
            event.getPayload().getTrustOnboardingSubmissionId()
        );
        var newStatement = new TrustStatementPartnerLinkDto();
        newStatement.setId(UUID.randomUUID());
        when(trustStatementService.issueAndPublishIdentityV1TrustStatement(any(), any())).thenReturn(newStatement);
        when(trustStatementService.issueAndPublishIdentityV2TrustStatement(any())).thenReturn(newStatement);

        when(
            trustOnboardingSubmissionApi.getTrustOnboardingSubmission(
                event.getPayload().getTrustOnboardingSubmissionId()
            )
        ).thenReturn(trustOnboardingSubmission);

        // when
        trustOnboardingSubmissionEventProcessor.processTiTrustOnboardingSubmissionAcceptedEvent(event);

        // then
        verifyTiTrustOnboardingSucceededEventWasSend(event.getPayload().getTrustOnboardingSubmissionId());
        TrustOnboardingTask task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            trustOnboardingSubmission.getId()
        );
        assertThat(task).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.ACCEPTED);
    }
}
