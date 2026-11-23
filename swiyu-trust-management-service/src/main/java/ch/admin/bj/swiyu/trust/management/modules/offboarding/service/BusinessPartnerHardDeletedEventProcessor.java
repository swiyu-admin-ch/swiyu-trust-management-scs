package ch.admin.bj.swiyu.trust.management.modules.offboarding.service;

import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerHardDeletedEvent;
import ch.admin.bj.swiyu.trust.management.modules.common.security.SecurityContextSupport;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Turns the {@code TiBusinessPartnerHardDeletedEvent} into a hard delete of the partner's TMS data.
 *
 * <p>The projection is synchronised first: the core business service has already deleted the partner, so the sync
 * reads a 404 and removes the local copy. Only then does {@link OffboardingService} start, and it aborts if the
 * projection is somehow still there - an event that arrives before the core business service is done must not delete
 * the data of a live partner.
 *
 * <p>Deliberately without {@code @Transactional}, like the comparable processor in the core business service: the
 * deletion spans two databases and manages its own transactions, and a redelivered event has to finish a partial run.
 * Failures propagate so the message is not acknowledged. See the comment on the handler method for why
 * {@code @IdempotentMessageHandler} is not used either.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPartnerHardDeletedEventProcessor {

    private final BusinessPartnerSyncService businessPartnerSyncService;
    private final OffboardingService offboardingService;

    // Deliberately not annotated with @IdempotentMessageHandler: its aspect requires an active transaction and records
    // the idempotence id in it. The annotation would therefore force this whole flow - two databases plus external
    // systems - into a single @Transactional method, in which a failure rolls back the marker but not the registry
    // deletions that already committed. Every deletion step is idempotent instead, so a redelivery converges and
    // finishes a partial run rather than being skipped.
    public void processBusinessPartnerHardDeletedEvent(TiBusinessPartnerHardDeletedEvent event) {
        var businessPartnerId = event.getPayload().getBusinessPartnerId();
        log.info("Received TiBusinessPartnerHardDeletedEvent for business partner '{}'", businessPartnerId);
        // The event carries no user trigger and the human origin is already in the task audit records, so the deletion
        // itself is audited as a system action.
        SecurityContextSupport.setSystemUserAuthentication();
        businessPartnerSyncService.syncPartner(businessPartnerId);
        offboardingService.hardDeleteBusinessPartner(businessPartnerId);
    }
}
