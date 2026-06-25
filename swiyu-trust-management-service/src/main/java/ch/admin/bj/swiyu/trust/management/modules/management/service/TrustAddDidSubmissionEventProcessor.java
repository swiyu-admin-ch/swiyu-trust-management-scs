package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionSubmittedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustAddDidsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.TrustAdditionalDidsSubmissionInternalDtoDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.PartnerName;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementDetails;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@AllArgsConstructor
public class TrustAddDidSubmissionEventProcessor {

    public static final String UNKNOWN_PARTNER_NAME = "Unknown";
    private final TrustAddDidTaskService taskService;
    private final TrustAddDidsSubmissionInternalApi trustAddDidsSubmissionApi;
    private final TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;
    private final TrustStatementService trustStatementService;

    @IdempotentMessageHandler
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
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

        // Look up partner info from the permissionDid's existing identity trust statement
        var existingIdentityStatement = trustStatementPartnerLinkRepository
            .findAllBySubjectAndTypeInAndStatus(
                permissionDid,
                List.of(
                    TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V1,
                    TrustStatementPartnerLinkType.TRUST_STATEMENT_IDENTITY_V2
                ),
                TrustStatementPartnerLinkStatus.ACTIVE
            )
            .stream()
            .findAny()
            .orElse(null);

        // Create the task
        PartnerName partnerName;
        UUID partnerId;
        if (existingIdentityStatement != null) {
            partnerId = existingIdentityStatement.getPartnerId();
            partnerName = buildPartnerNameFromDetails(existingIdentityStatement.getDetails());
        } else {
            partnerId = null;
            partnerName = unkownPartnerName();
        }

        var taskId = taskService.createTask(
            partnerId,
            partnerName,
            submissionId,
            permissionDid,
            submission.getUpdatedAt() != null ? submission.getUpdatedAt() : Instant.now(),
            getCurrentUserName()
        );

        // Validate permissionDid is trusted
        if (existingIdentityStatement == null) {
            log.warn(
                "Permission DID {} is not trusted (no active identity trust statement found). Rejecting.",
                permissionDid
            );
            taskService.reject(taskId, RejectReason.UNKNOWN);
            return;
        }

        var businessPartnerIdentity = BusinessPartnerIdentityMapper.toBusinessPartnerIdentity(
            existingIdentityStatement
        );

        // Issue identity trust statement for each new DID
        try {
            for (var didToAdd : submission.getDidsToAdd()) {
                var newDid = didToAdd.getDid();
                log.info("Issuing identity trust statement V1 or new DID: {}", newDid);
                var request = BusinessPartnerIdentityMapper.toTrustStatementPartnerLinkIdentityV1RequestDto(
                    newDid,
                    businessPartnerIdentity
                );
                trustStatementService.issueAndPublishIdentityV1TrustStatement(partnerId, request);

                log.info("Issuing identity trust statement V2 or new DID: {}", newDid);
                var request2 = BusinessPartnerIdentityMapper.toTrustStatementPartnerLinkIdentityV2RequestDto(
                    newDid,
                    businessPartnerIdentity
                );
                trustStatementService.issueAndPublishIdentityV2TrustStatement(request2);
            }
        } catch (Exception e) {
            log.error(
                "Failed to issue identity trust statement for add-DID submission {}. Rejecting.",
                submissionId,
                e
            );
            taskService.reject(taskId, RejectReason.UNKNOWN);
            return;
        }

        // All DIDs processed successfully
        taskService.accept(taskId);
        log.info("Trust Add DID submission {} processed successfully.", submissionId);
    }

    private static @NonNull PartnerName unkownPartnerName() {
        PartnerName partnerName;
        var name = UNKNOWN_PARTNER_NAME;
        partnerName = new PartnerName(name, name, name, name, name);
        return partnerName;
    }

    private PartnerName buildPartnerNameFromDetails(TrustStatementDetails details) {
        if (details instanceof IdentityV1Details identityDetails) {
            var entityName = identityDetails.getEntityName();
            return new PartnerName(
                entityName.getOrDefault(IdentityV1Details.Language.DE_CH, ""),
                entityName.getOrDefault(IdentityV1Details.Language.FR_CH, ""),
                entityName.getOrDefault(IdentityV1Details.Language.IT_CH, ""),
                entityName.getOrDefault(IdentityV1Details.Language.EN, ""),
                entityName.getOrDefault(IdentityV1Details.Language.RM_CH, "")
            );
        } else if (details instanceof IdentityV2Details identityDetails) {
            var entityName = identityDetails.getEntityName();
            return new PartnerName(
                entityName.getOrDefault(IdentityV2Details.Language.DE_CH, ""),
                entityName.getOrDefault(IdentityV2Details.Language.FR_CH, ""),
                entityName.getOrDefault(IdentityV2Details.Language.IT_CH, ""),
                entityName.getOrDefault(IdentityV2Details.Language.EN, ""),
                entityName.getOrDefault(IdentityV2Details.Language.RM_CH, "")
            );
        }
        return unkownPartnerName();
    }
}
