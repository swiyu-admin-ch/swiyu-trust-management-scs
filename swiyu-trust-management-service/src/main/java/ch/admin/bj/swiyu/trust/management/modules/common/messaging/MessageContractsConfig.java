package ch.admin.bj.swiyu.trust.management.modules.common.messaging;

import ch.admin.bit.jeap.messaging.annotations.JeapMessageConsumerContracts;
import ch.admin.bit.jeap.messaging.annotations.JeapMessageProducerContracts;
import ch.admin.bj.swiyu.messagetype.ti.*;
import org.springframework.context.annotation.Configuration;

@Configuration
@JeapMessageConsumerContracts(
    {
        TiVcSchemaSubmissionAcceptedEvent.TypeRef.class,
        TiTrustOnboardingSubmissionAcceptedEvent.TypeRef.class,
        TiTrustAddDidSubmissionSubmittedEvent.TypeRef.class,
    }
)
@JeapMessageProducerContracts(
    {
        TiVcSchemaPublicationSucceededEvent.TypeRef.class,
        TiVcSchemaPublicationFailedEvent.TypeRef.class,
        TiTrustOnboardingSucceededEvent.TypeRef.class,
        TiTrustOnboardingRejectedEvent.TypeRef.class,
        TiTrustOnboardingInformationRequestedEvent.TypeRef.class,
        TiTrustAddDidSubmissionAcceptedEvent.TypeRef.class,
        TiTrustAddDidSubmissionRejectedEvent.TypeRef.class,
    }
)
public class MessageContractsConfig {}
