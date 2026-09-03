package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.management.config.DefaultIdentityProperties;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPartnerIdentityDomainService {

    private final BusinessPartnerIdentityRepository businessPartnerIdentityRepository;
    private final DefaultIdentityProperties defaultIdentityProperties;

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public BusinessPartnerIdentity activate(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));

        bpi.activate(defaultIdentityProperties.validity());

        return businessPartnerIdentityRepository.save(bpi);
    }

    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public BusinessPartnerIdentity deactivate(UUID businessPartnerId) {
        var bpi = businessPartnerIdentityRepository
            .findById(businessPartnerId)
            .orElseThrow(businessPartnerIdentityNotFound(businessPartnerId));

        bpi.deactivate();
        return businessPartnerIdentityRepository.save(bpi);
    }

    private static Supplier<ResourceNotFoundException> businessPartnerIdentityNotFound(UUID id) {
        return () -> new ResourceNotFoundException("No business partner identity found for id %s".formatted(id));
    }
}
