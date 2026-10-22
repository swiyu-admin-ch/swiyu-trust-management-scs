package ch.admin.bj.swiyu.trust.management.modules.management.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.service.BusinessPartnerIdentityMapper.toBusinessPartnerIdentityStatusDto;

import ch.admin.bj.swiyu.trust.management.modules.management.api.BusinessPartnerViewListItemDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerView;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BusinessPartnerViewMapper {

    public static BusinessPartnerViewListItemDto toBusinessPartnerViewListItemDto(BusinessPartnerView partner) {
        return new BusinessPartnerViewListItemDto(
            partner.getBusinessPartnerId(),
            partner.getPartnerName(),
            partner.getStatus() == null ? null : toBusinessPartnerIdentityStatusDto(partner.getStatus()),
            partner.getValidUntil()
        );
    }
}
