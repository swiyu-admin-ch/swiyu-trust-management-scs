package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.api.VcSchemaSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.model.VcSchemaSubmissionDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaAlreadyExistsException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaPublicationFailedException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaUrlValidationFailedException;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiVcSchemaPublishingFailedEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiVcSchemaPublishingSucceededEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.VcSchemaService;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@AllArgsConstructor
public class VcSchemaSubmissionEventProcessor {

    private final VcSchemaService vcSchemaService;
    private final VcSchemaSubmissionApi vcSchemaSubmissionInternalApiApi;
    private final VcSchemaPartnerLinkService vcSchemaPartnerLinkService;
    private final OutboxEventPublisher outboxEventPublisher;

    @IdempotentMessageHandler
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void processVcSchemaSubmissionAccepted(TiVcSchemaSubmissionAcceptedEvent event) {
        var submissionId = event.getPayload().getVcSchemaSubmissionId();
        log.info("Retrieve VC Schema Submission with ID: {}", submissionId);
        try {
            var vcSchemaSubmission = getVcSchemaSubmission(submissionId);
            var vcSchema = vcSchemaService.publishVcSchema(vcSchemaSubmission.getFile());
            vcSchemaPartnerLinkService.createVcSchemaPartnerLink(
                vcSchema.id(),
                vcSchemaSubmission.getId(),
                vcSchemaSubmission.getPartnerId()
            );
            outboxEventPublisher.publishVcSchemaPublicationSucceededEvent(
                TiVcSchemaPublishingSucceededEventBuilder.create().vcSchemaSubmissionId(submissionId).build()
            );
        } catch (
            VcSchemaPublicationFailedException
            | VcSchemaUrlValidationFailedException
            | VcSchemaAlreadyExistsException e
        ) {
            log.info("Failed to process VC Schema Submission with ID: {}", submissionId, e);
            outboxEventPublisher.publishVcSchemaPublicationFailedEvent(
                TiVcSchemaPublishingFailedEventBuilder.create()
                    .vcSchemaSubmissionId(submissionId)
                    .failureReason(e.getMessage())
                    .build()
            );
        }
    }

    private VcSchemaSubmissionDto getVcSchemaSubmission(UUID submissionId) throws VcSchemaPublicationFailedException {
        try {
            return vcSchemaSubmissionInternalApiApi.getVcSchemaSubmission(submissionId);
        } catch (RestClientResponseException e) {
            throw new ExternalSystemException(e.getMessage(), ExternalSystem.CORE_BUSINESS_SERVICE, e.getStatusCode());
        }
    }
}
