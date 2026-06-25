package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.consumer;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustOnboardingTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustTaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingSubmissionEventProcessor;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@TestPropertySource(properties = "app.functionality.automaticApprovalEnabled=false")
class TrustOnboardingSubmissionEventProcessorAutomaticApprovalDisabledIT {

    @Autowired
    TrustOnboardingSubmissionEventProcessor trustOnboardingSubmissionEventProcessor;

    @Autowired
    TrustOnboardingTaskRepository trustOnboardingTaskRepository;

    @MockitoBean
    TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;

    @Test
    @WithJeapAuthenticationToken(username = "test")
    @ExtendWith(OutputCaptureExtension.class)
    void whenAutomaticApprovalDisabled_taskRemainsInOpenedState(CapturedOutput output) {
        // given
        var event = TrustOnboardingTestData.tiTrustOnboardingSubmissionAcceptedEvent();
        var trustOnboardingSubmission = TrustOnboardingTestData.trustOnboardingSubmissionDto();

        when(
            trustOnboardingSubmissionApi.getTrustOnboardingSubmission(
                event.getPayload().getTrustOnboardingSubmissionId()
            )
        ).thenReturn(trustOnboardingSubmission);

        // when
        trustOnboardingSubmissionEventProcessor.processTiTrustOnboardingSubmissionAcceptedEvent(event);

        // then
        assertThat(output.getOut())
            .contains("Retrieve Trust Onboarding Submission Accepted Event with ID:")
            .contains(event.getPayload().getTrustOnboardingSubmissionId().toString());

        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            trustOnboardingSubmission.getId()
        );
        assertThat(task).isNotNull();
        assertThat(task.getStatus()).isEqualTo(TrustTaskStatus.OPENED);
    }
}
