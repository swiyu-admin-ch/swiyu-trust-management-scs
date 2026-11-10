package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.consumer;

import static ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper.today;
import static ch.admin.bj.swiyu.trust.management.test.BusinessPartnerIdentityTestData.businessPartnerIdentityExpiringIn30Days;
import static ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData.trustOnboardingSubmissionDto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.admin.bit.jeap.security.test.WithJeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustOnboardingSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TaskType;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.task.TrustOnboardingTaskRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingSubmissionEventProcessor;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TestRepositories;
import ch.admin.bj.swiyu.trust.management.test.TrustOnboardingTestData;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * A renewal is approved automatically regardless of the automatic-approval flag, which is off on every stage except
 * the integration ones - so the flag-off case is the one that needed covering.
 */
@ActiveProfiles("test")
@SpringBootTest
@EmbeddedKafka
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@TestPropertySource(properties = "app.functionality.automaticApprovalEnabled=false")
class TrustOnboardingSubmissionEventProcessorRenewalIT {

    @Autowired
    TrustOnboardingSubmissionEventProcessor trustOnboardingSubmissionEventProcessor;

    @Autowired
    TrustOnboardingTaskRepository trustOnboardingTaskRepository;

    @Autowired
    TestRepositories testRepositories;

    @MockitoBean
    TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;

    @MockitoBean
    TrustStatementService trustStatementService;

    @Test
    @WithJeapAuthenticationToken(username = "test")
    void whenAutomaticApprovalDisabled_renewalIsStillApprovedAndExtendsTheIdentity() {
        // given an active identity that is about to expire, which is why the partner renews
        var bpi = testRepositories.businessPartnerIdentity.save(businessPartnerIdentityExpiringIn30Days());
        var expiryBeforeRenewal = bpi.getValidUntil();

        var newStatement = new TrustStatementPartnerLinkDto();
        newStatement.setId(UUID.randomUUID());
        when(trustStatementService.issueAndPublishIdentityV1TrustStatement(any(), any())).thenReturn(newStatement);
        when(trustStatementService.issueAndPublishIdentityV2TrustStatement(any())).thenReturn(newStatement);

        var event = TrustOnboardingTestData.tiTrustOnboardingSubmissionAcceptedEvent();
        var submission = trustOnboardingSubmissionDto(event.getPayload().getTrustOnboardingSubmissionId())
            .partnerId(bpi.getId())
            .type(TrustOnboardingSubmissionDto.TypeEnum.RENEWAL);
        when(
            trustOnboardingSubmissionApi.getTrustOnboardingSubmission(
                event.getPayload().getTrustOnboardingSubmissionId()
            )
        ).thenReturn(submission);

        // when
        trustOnboardingSubmissionEventProcessor.processTiTrustOnboardingSubmissionAcceptedEvent(event);

        // then the task was approved without anyone acting on it
        var task = trustOnboardingTaskRepository.getTrustOnboardingTaskByTrustOnboardingSubmissionId(
            submission.getId()
        );
        assertThat(task).isNotNull();
        assertThat(task.getTaskType()).isEqualTo(TaskType.RENEWAL);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ACCEPTED);

        // and the identity stayed active with a validity reaching beyond the old expiry
        var renewed = testRepositories.businessPartnerIdentity.findById(bpi.getId()).orElseThrow();
        assertThat(renewed.getStatus()).isEqualTo(BusinessPartnerIdentityStatus.ACTIVE);
        assertThat(renewed.getValidUntil()).isAfter(expiryBeforeRenewal).isEqualTo(today().plusYears(3).toInstant());
    }
}
