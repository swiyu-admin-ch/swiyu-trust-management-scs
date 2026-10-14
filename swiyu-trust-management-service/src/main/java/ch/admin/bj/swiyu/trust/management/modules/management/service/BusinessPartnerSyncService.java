package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;

import ch.admin.bj.swiyu.trust.client.core.business.internal.api.BusinessPartnerV2Api;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerDto;
import ch.admin.bj.swiyu.trust.client.core.business.internal.model.BusinessPartnerTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystem;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.ExternalSystemException;
import ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapUtil;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartner;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerRepository;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerType;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;

/**
 * Keeps the local business partner projection in step with the core business service.
 *
 * <p>Synchronises exactly one partner and does not know who asked, so the same service serves the
 * {@code TiBusinessPartnerUpdatedEvent} today and the nightly reconciliation later.
 *
 * <p>The event carries nothing but the partner id by design. The authoritative state is read here instead, so the
 * projection can never be half-updated and no partner data travels over the broker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessPartnerSyncService {

    private final BusinessPartnerV2Api businessPartnerApi;
    private final BusinessPartnerRepository businessPartnerRepository;

    /** Reads the partner from the core business service and upserts the projection. */
    @Transactional(transactionManager = MANAGEMENT_TRANSACTION_MANAGER)
    public void syncPartner(UUID businessPartnerId) {
        BusinessPartnerDto dto;
        try {
            dto = businessPartnerApi.getBusinessPartner(businessPartnerId);
        } catch (RestClientResponseException e) {
            if (HttpStatus.NOT_FOUND.isSameCodeAs(e.getStatusCode())) {
                // Not an error to retry: the partner is gone, so the projection follows. Were this rethrown, the
                // event would never be acknowledged and come back forever.
                log.info("Business partner '{}' is unknown to CBS, removing it from the projection", businessPartnerId);
                businessPartnerRepository.deleteById(businessPartnerId);
                return;
            }
            throw new ExternalSystemException(e.getMessage(), ExternalSystem.CORE_BUSINESS_SERVICE, e.getStatusCode());
        }

        var partner = businessPartnerRepository
            .findById(businessPartnerId)
            .orElseGet(() -> BusinessPartner.createFor(businessPartnerId));

        partner.sync(
            LocalizedMapUtil.getDefaultValue(dto.getEntityName()),
            dto.getUid(),
            toBusinessPartnerType(dto.getType()),
            Boolean.TRUE.equals(dto.getHardDeleteAllowed()),
            Instant.now()
        );

        businessPartnerRepository.save(partner);
        log.info("Synchronised the projection of business partner '{}'", businessPartnerId);
    }

    private static BusinessPartnerType toBusinessPartnerType(BusinessPartnerTypeDto type) {
        if (type == null) {
            throw new IllegalStateException("Business partner type is mandatory but was missing in the CBS response");
        }
        return switch (type) {
            case GOVERNMENTAL_INSTITUTION -> BusinessPartnerType.GOVERNMENTAL_INSTITUTION;
            case BUSINESS -> BusinessPartnerType.BUSINESS;
            case INDIVIDUAL -> BusinessPartnerType.INDIVIDUAL;
        };
    }
}
