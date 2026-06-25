package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.consumer;

import ch.admin.bj.swiyu.messagetype.ti.TiVcSchemaSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.trust.management.modules.management.service.VcSchemaSubmissionEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VcSchemaSubmissionEventConsumer {

    private final VcSchemaSubmissionEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiVcSchemaSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiVcSchemaSubmissionAcceptedEventListener"
    )
    public void receive(TiVcSchemaSubmissionAcceptedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processVcSchemaSubmissionAccepted(event);
        ack.acknowledge();
    }
}
