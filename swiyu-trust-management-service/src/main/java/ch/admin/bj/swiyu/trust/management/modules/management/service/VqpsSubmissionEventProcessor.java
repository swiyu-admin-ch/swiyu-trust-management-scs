package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.date.DateTimeHelper.today;
import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.messagetype.ti.TiVqpsSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.VqpsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.VqpsSubmissionInternalDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.VerificationQueryV2RequestDto;
import ch.admin.bj.swiyu.trust.management.modules.management.config.statements.DefaultStatementProperties;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.event.TiVqpsPublicationSucceededEventBuilder;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.publisher.OutboxEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class VqpsSubmissionEventProcessor {

    private static final String VERIFICATION_REQUEST_TYPE = "DCQL";

    private final TrustStatementService trustStatementService;
    private final VqpsSubmissionInternalApi vqpsSubmissionInternalApi;
    private final OutboxEventPublisher outboxEventPublisher;
    private final DefaultStatementProperties defaultStatementProperties;
    private final ObjectMapper objectMapper;

    @IdempotentMessageHandler
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void processVqpsSubmissionAccepted(TiVqpsSubmissionAcceptedEvent event) {
        var submissionId = event.getPayload().getVqpsSubmissionId();
        log.info("Processing VQPS Submission with ID: {}", submissionId);
        var submission = vqpsSubmissionInternalApi.getVqpsSubmission(submissionId);
        var encodedVqps = trustStatementService
            .issueAndPublishVerificationQueryV2TrustStatement(toVerificationQueryV2RequestDto(submission))
            .encodedVqps();
        outboxEventPublisher.publishVqpsPublicationSucceededEvent(
            TiVqpsPublicationSucceededEventBuilder.create().vqpsSubmissionId(submissionId).vqps(encodedVqps).build()
        );
    }

    private VerificationQueryV2RequestDto toVerificationQueryV2RequestDto(VqpsSubmissionInternalDto submission) {
        ZonedDateTime now = today();
        return new VerificationQueryV2RequestDto(
            submission.getPartnerId(),
            submission.getSub(),
            now.toInstant(),
            now.plus(defaultStatementProperties.timeToLive()).toInstant(),
            submission.getPurposeName(),
            submission.getPurposeDescription(),
            new VerificationQueryV2RequestDto.VerificationRequestObjectDto(
                VERIFICATION_REQUEST_TYPE,
                submission.getScope(),
                objectMapper.valueToTree(submission.getQuery())
            )
        );
    }
}
