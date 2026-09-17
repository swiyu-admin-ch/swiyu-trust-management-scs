package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserFullName;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;

import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionSubmittedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustAddDidsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustAdditionalDidsSubmissionInternalDtoDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@AllArgsConstructor
public class TrustAddDidSubmissionEventProcessor {

    private final TrustAddDidTaskService taskService;
    private final TrustAddDidsSubmissionInternalApi trustAddDidsSubmissionApi;
    private final BusinessPartnerIdentityService businessPartnerIdentityService;

    public void processTiTrustAddDidSubmissionSubmittedEvent(TiTrustAddDidSubmissionSubmittedEvent event) {
        var submissionId = event.getPayload().getTrustAddDidSubmissionId();
        log.info("Processing Trust Add DID Submission Submitted Event with ID: {}", submissionId);

        TrustAdditionalDidsSubmissionInternalDtoDto submission;
        try {
            submission = trustAddDidsSubmissionApi.getSubmission(submissionId);
        } catch (RestClientResponseException exception) {
            throw new ExternalSystemException(
                exception.getMessage(),
                ExternalSystem.CORE_BUSINESS_SERVICE,
                exception.getStatusCode()
            );
        }
        var permissionDid = submission.getPermissionDid().getDid();

        var businessPartnerIdentity = businessPartnerIdentityService.getBusinessPartnerIdentityByTrustedIdentifier(
            permissionDid
        );

        var taskId = taskService.createTask(
            businessPartnerIdentity.id(),
            businessPartnerIdentity.entityName(),
            submissionId,
            permissionDid,
            submission.getUpdatedAt() != null ? submission.getUpdatedAt() : Instant.now(),
            getCurrentUserName()
        );
        taskService.approve(taskId, getCurrentUserFullName());

        log.info("Trust Add DID submission {} processed successfully.", submissionId);
    }
}
