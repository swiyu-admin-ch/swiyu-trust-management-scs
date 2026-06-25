package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.consumer;

import ch.admin.bj.swiyu.messagetype.ti.TiVqpsSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.trust.management.modules.management.service.VqpsSubmissionEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VqpsSubmissionEventConsumer {

    private final VqpsSubmissionEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiVqpsSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiVqpsSubmissionAcceptedEventListener"
    )
    public void receive(TiVqpsSubmissionAcceptedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processVqpsSubmissionAccepted(event);
        ack.acknowledge();
    }
}
