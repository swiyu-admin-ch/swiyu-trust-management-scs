package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bit.jeap.messaging.idempotence.messagehandler.IdempotentMessageHandler;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityActivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityDeactivatedEvent;
import ch.admin.bj.swiyu.messagetype.ti.TiBusinessPartnerIdentityUpdatedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class BusinessPartnerIdentityEventProcessor {

    private final BusinessPartnerIdentityService businessPartnerIdentityService;

    @IdempotentMessageHandler
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void processTiBusinessPartnerIdentityActivatedEvent(TiBusinessPartnerIdentityActivatedEvent event) {
        var businessPartnerId = event.getPayload().getBusinessPartnerIdentityId();
        log.info("Retrieve Business Partner Identity Activated Event with ID: {}", businessPartnerId);
        var reason = "Business Partner Identity has been activated";
        businessPartnerIdentityService.deactivateTrustStatements(businessPartnerId, reason);
        businessPartnerIdentityService.issueTrustStatements(businessPartnerId);
    }

    @IdempotentMessageHandler
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void processTiBusinessPartnerIdentityDeactivatedEvent(TiBusinessPartnerIdentityDeactivatedEvent event) {
        var businessPartnerId = event.getPayload().getBusinessPartnerIdentityId();
        var reason = "Business Partner Identity has been deactivated";
        log.info(
            "Retrieve Business Partner Identity Deactivated Event with ID: {} and reason: {}",
            businessPartnerId,
            reason
        );
        businessPartnerIdentityService.deactivateTrustStatements(businessPartnerId, reason);
    }

    @IdempotentMessageHandler
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void processTiBusinessPartnerIdentityUpdatedEvent(TiBusinessPartnerIdentityUpdatedEvent event) {
        var businessPartnerId = event.getPayload().getBusinessPartnerIdentityId();
        log.info("Retrieve Business Partner Identity Updated Event with ID: {}", businessPartnerId);
        businessPartnerIdentityService.issueTrustStatements(businessPartnerId);
    }
}
