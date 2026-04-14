package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport.getCurrentUserName;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.messagetype.ti.RejectReason;
import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionSubmittedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.api.TrustAddDidsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.model.TrustAdditionalDidsSubmissionInternalDtoDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.management.api.TrustStatementPartnerLinkIdentityV1RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.*;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementDetails;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementType;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@AllArgsConstructor
public class TrustAddDidSubmissionEventProcessor {

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
        var existingIdentityStatement = trustStatementPartnerLinkRepository.findBySubjectAndTypeAndStatus(
            permissionDid,
            TrustStatementType.TRUST_STATEMENT_IDENTITY_V1,
            TrustStatementPartnerLinkStatus.ACTIVE
        );

        // Create the task
        PartnerName partnerName;
        UUID partnerId;
        if (existingIdentityStatement.isPresent()) {
            var statement = existingIdentityStatement.get();
            partnerId = statement.getPartnerId();
            partnerName = buildPartnerNameFromDetails(statement.getDetails());
        } else {
            partnerId = null;
            partnerName = new PartnerName("Unknown", "Unknown", "Unknown", "Unknown", "Unknown");
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
        if (existingIdentityStatement.isEmpty()) {
            log.warn(
                "Permission DID {} is not trusted (no active identity trust statement found). Rejecting.",
                permissionDid
            );
            taskService.reject(taskId, RejectReason.UNKNOWN);
            return;
        }

        var statement = existingIdentityStatement.get();

        // Issue identity trust statement for each new DID
        try {
            for (var didToAdd : submission.getDidsToAdd()) {
                var newDid = didToAdd.getDid();
                log.info("Issuing identity trust statement for new DID: {}", newDid);

                var identityDetails = (IdentityV1Details) statement.getDetails();
                var entityNameMap = toRequestLanguageMap(identityDetails.getEntityName());

                var request = new TrustStatementPartnerLinkIdentityV1RequestDto(
                    newDid,
                    Instant.now(),
                    Instant.now().plus(Duration.ofDays(365)),
                    entityNameMap,
                    identityDetails.getIsStateActor(),
                    toRequestRegistryIds(identityDetails.getRegistryIds())
                );
                trustStatementService.issueAndPublishIdentityTrustStatement(partnerId, request);
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
        }
        return new PartnerName("Unknown", "Unknown", "Unknown", "Unknown", "Unknown");
    }

    private Map<TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto, String> toRequestLanguageMap(
        Map<IdentityV1Details.Language, String> source
    ) {
        var result = new HashMap<TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto, String>();
        if (source == null) return result;
        for (var entry : source.entrySet()) {
            var lang = switch (entry.getKey()) {
                case EN -> TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.EN;
                case DE_CH -> TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.DE_CH;
                case FR_CH -> TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.FR_CH;
                case IT_CH -> TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.IT_CH;
                case RM_CH -> TrustStatementPartnerLinkIdentityV1RequestDto.LanguageDto.RM_CH;
            };
            result.put(lang, entry.getValue());
        }
        return result;
    }

    private java.util.List<TrustStatementPartnerLinkIdentityV1RequestDto.RegistryIdDto> toRequestRegistryIds(
        java.util.List<IdentityV1Details.RegistryId> source
    ) {
        if (source == null) return java.util.List.of();
        return source
            .stream()
            .map(r -> new TrustStatementPartnerLinkIdentityV1RequestDto.RegistryIdDto(r.type(), r.value()))
            .toList();
    }
}
