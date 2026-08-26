package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionAcceptedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class ProtectedVerificationSubmissionEventProcessor {

    private final ProtectedVerificationRequestTaskService taskService;

    @IdempotentMessageHandler
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void processTiProtectedVerificationSubmissionAcceptedEvent(
        TiProtectedVerificationSubmissionAcceptedEvent event
    ) {
        var submissionId = event.getPayload().getProtectedVerificationSubmissionId();
        log.info("Processing Protected Verification Submission Accepted Event with ID: {}", submissionId);
        taskService.createTask(submissionId, getCurrentUserName());
    }
}
