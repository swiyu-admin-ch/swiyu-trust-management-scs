package ch.admin.bj.swiyu.trust.management.modules.offboarding.infrastructure.consumer;

import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerHardDeletedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.trust.management.modules.offboarding.service.BusinessPartnerHardDeletedEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessPartnerHardDeletedEventConsumer {

    private final BusinessPartnerHardDeletedEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(topics = { TiBusinessPartnerHardDeletedEvent.TypeRef.DEFAULT_TOPIC })
    public void receive(TiBusinessPartnerHardDeletedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processBusinessPartnerHardDeletedEvent(event);
        ack.acknowledge();
    }
}
