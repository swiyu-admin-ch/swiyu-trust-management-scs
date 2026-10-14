package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Turns the {@code TiBusinessPartnerUpdatedEvent} into a sync of the local projection.
 *
 * <p>Deliberately without {@code @IdempotentMessageHandler}, unlike the neighbouring processors: the event is a nudge
 * without a payload, so synchronising twice only reads the same state twice. That handler is needed where a repeat
 * would duplicate something, which an upsert cannot. Leaving it off also keeps a redelivery useful rather than
 * skipped, which is what one wants from a projection that may have drifted.
 *
 * <p>No transaction here either: {@link BusinessPartnerSyncService#syncPartner} opens its own on the management
 * datasource.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPartnerEventProcessor {

    private final BusinessPartnerSyncService businessPartnerSyncService;

    public void processBusinessPartnerUpdatedEvent(TiBusinessPartnerUpdatedEvent event) {
        var businessPartnerId = event.getPayload().getBusinessPartnerId();
        log.info("Received TiBusinessPartnerUpdatedEvent for business partner '{}'", businessPartnerId);
        businessPartnerSyncService.syncPartner(businessPartnerId);
    }
}
