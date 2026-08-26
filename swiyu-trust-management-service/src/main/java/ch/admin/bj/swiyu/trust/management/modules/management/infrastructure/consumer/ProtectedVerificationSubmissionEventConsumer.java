package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.consumer;

import ch.admin.bj.swiyu.messagetype.ti.TiProtectedVerificationSubmissionAcceptedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedVerificationSubmissionEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProtectedVerificationSubmissionEventConsumer {

    private final ProtectedVerificationSubmissionEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(topics = { TiProtectedVerificationSubmissionAcceptedEvent.TypeRef.DEFAULT_TOPIC })
    public void receive(TiProtectedVerificationSubmissionAcceptedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processTiProtectedVerificationSubmissionAcceptedEvent(event);
        ack.acknowledge();
    }
}
