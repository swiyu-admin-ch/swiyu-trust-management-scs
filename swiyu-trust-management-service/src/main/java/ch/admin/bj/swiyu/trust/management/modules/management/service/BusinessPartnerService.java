package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Operations on the local projection of the business partners owned by the core business service.
 */
@Service
@RequiredArgsConstructor
public class BusinessPartnerService {

    private final BusinessPartnerRepository businessPartnerRepository;

    /**
     * Whether the partner is still known locally. The hard-delete flow uses this as its safeguard: an event that
     * arrives before the core business service has finished its own deletion re-creates the projection on sync, and
     * the offboarding must then stop instead of removing data of a live partner.
     */
    @Transactional(readOnly = true, transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public boolean partnerExists(UUID partnerId) {
        return businessPartnerRepository.existsById(partnerId);
    }
}
