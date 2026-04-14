package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.consumer;

import ch.admin.bj.swiyu.messagetype.ti.TiTrustOnboardingSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustOnboardingSubmissionEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrustOnboardingSubmissionEventConsumer {

    private final TrustOnboardingSubmissionEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiTrustOnboardingSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiTrustOnboardingSubmissionAcceptedEventListener"
    )
    public void receive(TiTrustOnboardingSubmissionAcceptedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processTiTrustOnboardingSubmissionAcceptedEvent(event);
        ack.acknowledge();
    }
}
