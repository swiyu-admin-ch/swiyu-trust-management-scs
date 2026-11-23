package ch.admin.bj.swiyu.trust.management.modules.offboarding.service;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerDeletionError;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.BusinessPartnerNotDeletableException;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.DomainEventService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorPublicationService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.NonCompliantActorService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.ProtectedIssuanceService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TaskService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.TrustStatementService;
import ch.admin.bj.swiyu.trust.management.modules.management.service.VcSchemaPartnerLinkService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Removes all TMS-owned data of a business partner the core business service has already hard deleted.
 *
 * <p>The flow spans two databases and several external systems, so there is no transaction around it. Each aggregate
 * is deleted in its own transaction, in the order below, and every step is written to tolerate being already done, so
 * a redelivered event finishes what a failed run left behind instead of starting over.
 *
 * <p>Order matters: the registry rows go before the management rows that point at them, trust statements are
 * deactivated before they are tombstoned, and the partner identity goes last. The projection is already gone here -
 * the processor synchronised it from the core business service before calling this method.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OffboardingService {

    private final BusinessPartnerService businessPartnerService;
    private final TrustStatementService trustStatementService;
    private final VcSchemaPartnerLinkService vcSchemaPartnerLinkService;
    private final NonCompliantActorService nonCompliantActorService;
    private final NonCompliantActorPublicationService nonCompliantActorPublicationService;
    private final ProtectedIssuanceService protectedIssuanceService;
    private final DomainEventService domainEventService;
    private final TaskService taskService;
    private final BusinessPartnerIdentityService businessPartnerIdentityService;

    /**
     * @throws BusinessPartnerNotDeletableException when the
     *     partner still exists locally, i.e. the event arrived before the core business service finished its deletion
     *     or out of order. The Kafka message is then not acknowledged and is delivered again later.
     */
    public void hardDeleteBusinessPartner(UUID partnerId) {
        if (businessPartnerService.partnerExists(partnerId)) {
            throw new BusinessPartnerNotDeletableException(
                BusinessPartnerDeletionError.PARTNER_STILL_EXISTS,
                "Business partner %s still exists, the hard-deleted event is premature or out of order".formatted(
                    partnerId
                )
            );
        }

        log.info("Starting hard delete of business partner '{}'", partnerId);
        trustStatementService.hardDeleteByPartnerId(partnerId);
        vcSchemaPartnerLinkService.hardDeleteByPartnerId(partnerId);
        if (nonCompliantActorService.hardDeleteByPartnerId(partnerId) > 0) {
            // Asynchronous on purpose: the deletion must not fail on a list republication, the scheduled refresh
            // catches up. Only when something was deleted, so a redelivery does not republish an unchanged list.
            nonCompliantActorPublicationService.triggerPublicationAsync();
        }
        protectedIssuanceService.hardDeleteByPartnerId(partnerId);
        // The event log is deleted before the tasks because its rows reference them.
        domainEventService.hardDeleteByPartnerId(partnerId);
        taskService.hardDeleteByPartnerId(partnerId);
        businessPartnerIdentityService.hardDeleteByPartnerId(partnerId);
        log.info("Hard delete of business partner '{}' completed", partnerId);
    }
}
