package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.management.config.FunctionalityProperties;
import ch.admin.bj.swiyu.trust.management.modules.ui.api.taskaction.ApproveTaskActionDto;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@AllArgsConstructor
public class TrustOnboardingSubmissionEventProcessor {

    private final TrustOnboardingTaskService taskService;
    private final TrustOnboardingSubmissionApi trustOnboardingSubmissionApi;
    private final FunctionalityProperties functionalityProperties;

    @IdempotentMessageHandler
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void processTiTrustOnboardingSubmissionAcceptedEvent(TiTrustOnboardingSubmissionAcceptedEvent event) {
        log.info(
            "Retrieve Trust Onboarding Submission Accepted Event with ID: {}",
            event.getPayload().getTrustOnboardingSubmissionId()
        );
        try {
            var trustOnboardingSubmission = this.trustOnboardingSubmissionApi.getTrustOnboardingSubmission(
                event.getPayload().getTrustOnboardingSubmissionId()
            );
            var taskId = this.taskService.createTaskByTrustOnboardingSubmission(
                trustOnboardingSubmission,
                getCurrentUserName()
            );
            if (functionalityProperties.automaticApprovalEnabled()) {
                this.taskService.approve(
                    taskId,
                    new ApproveTaskActionDto(
                        "Automatically approved by system configuration",
                        "Automatic approval by system"
                    ),
                    getCurrentUserName()
                );
            }
        } catch (RestClientResponseException exception) {
            throw new ExternalSystemException(
                exception.getMessage(),
                ExternalSystem.CORE_BUSINESS_SERVICE,
                exception.getStatusCode()
            );
        }
    }
}
