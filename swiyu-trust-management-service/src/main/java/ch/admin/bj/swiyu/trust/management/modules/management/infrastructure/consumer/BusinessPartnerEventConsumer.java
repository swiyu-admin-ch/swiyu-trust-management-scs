package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.consumer;

import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerUpdatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessPartnerEventConsumer {

    private final BusinessPartnerEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(topics = { TiBusinessPartnerUpdatedEvent.TypeRef.DEFAULT_TOPIC })
    public void receive(TiBusinessPartnerUpdatedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processBusinessPartnerUpdatedEvent(event);
        ack.acknowledge();
    }
}
