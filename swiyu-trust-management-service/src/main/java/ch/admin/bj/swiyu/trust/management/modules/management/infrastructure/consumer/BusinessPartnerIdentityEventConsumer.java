package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.consumer;

import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.MessagingSecurityContext;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityEventProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BusinessPartnerIdentityEventConsumer {

    private final BusinessPartnerIdentityEventProcessor processor;
    private final MessagingSecurityContext messagingSecurityContext;

    @KafkaListener(
        topics = { TiBusinessPartnerIdentityActivatedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiBusinessPartnerIdentityActivatedEventListener"
    )
    public void receiveActivated(TiBusinessPartnerIdentityActivatedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processTiBusinessPartnerIdentityActivatedEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiBusinessPartnerIdentityDeactivatedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiBusinessPartnerIdentityDeactivatedEventListener"
    )
    public void receiveDeactivated(TiBusinessPartnerIdentityDeactivatedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processTiBusinessPartnerIdentityDeactivatedEvent(event);
        ack.acknowledge();
    }

    @KafkaListener(
        topics = { TiBusinessPartnerIdentityUpdatedEvent.TypeRef.DEFAULT_TOPIC },
        id = "TiBusinessPartnerIdentityUpdatedEventListener"
    )
    public void receiveUpdated(TiBusinessPartnerIdentityUpdatedEvent event, Acknowledgment ack) {
        messagingSecurityContext.setPreferredUser(event.getPublisher());
        processor.processTiBusinessPartnerIdentityUpdatedEvent(event);
        ack.acknowledge();
    }
}
