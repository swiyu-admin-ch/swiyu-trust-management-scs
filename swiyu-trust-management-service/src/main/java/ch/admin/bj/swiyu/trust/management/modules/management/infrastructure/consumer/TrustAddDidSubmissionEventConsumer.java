package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.consumer;

import ch.admin.bj.swiyu.messagetype.ti.TiTrustAddDidSubmissionSubmittedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustAddDidSubmissionEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrustAddDidSubmissionEventConsumer {

    private final TrustAddDidSubmissionEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiTrustAddDidSubmissionSubmittedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiTrustAddDidSubmissionSubmittedEventListener"
    )
    public void receive(TiTrustAddDidSubmissionSubmittedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processTiTrustAddDidSubmissionSubmittedEvent(event);
        ack.acknowledge();
    }
}
