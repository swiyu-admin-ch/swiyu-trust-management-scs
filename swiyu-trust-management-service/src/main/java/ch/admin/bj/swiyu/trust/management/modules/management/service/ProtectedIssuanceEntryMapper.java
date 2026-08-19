package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceAuthorizationDto;
import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceEntryDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceAuthorization;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceEntry;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtectedIssuanceEntryMapper {

    public static ProtectedIssuanceEntryDto toProtectedIssuanceEntryDto(ProtectedIssuanceEntry protectedIssuanceEntry) {
        return new ProtectedIssuanceEntryDto(
            protectedIssuanceEntry.getId(),
            protectedIssuanceEntry.getVct(),
            protectedIssuanceEntry.getName(),
            protectedIssuanceEntry.getProtectedAt()
        );
    }

    public static ProtectedIssuanceAuthorizationDto toProtectedIssuanceAuthorizationDto(
        ProtectedIssuanceAuthorization authorization,
        ProtectedIssuanceEntry entry
    ) {
        return new ProtectedIssuanceAuthorizationDto(
            authorization.getId(),
            authorization.getBusinessPartnerIdentityId(),
            authorization.getProtectedIssuanceEntryId(),
            entry != null ? entry.getVct() : null,
            entry != null ? entry.getName() : null,
            authorization.getReason()
        );
    }

    public static List<String> toProtectedIssuanceV2Details(List<ProtectedIssuanceEntry> protectedElements) {
        return protectedElements.stream().map(ProtectedIssuanceEntry::getVct).toList();
    }
}
