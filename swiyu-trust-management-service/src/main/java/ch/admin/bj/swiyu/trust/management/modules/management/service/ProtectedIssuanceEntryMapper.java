package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.management.api.ProtectedIssuanceEntryDto;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.ProtectedIssuanceEntry;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ProtectedIssuanceEntryMapper {

    public static ProtectedIssuanceEntryDto toProtectedIssuanceEntryDto(ProtectedIssuanceEntry protectedIssuanceEntry) {
        return new ProtectedIssuanceEntryDto(
            protectedIssuanceEntry.getId(),
            protectedIssuanceEntry.getVct(),
            protectedIssuanceEntry.getProtectedAt()
        );
    }

    public static List<String> toProtectedIssuanceV2Details(List<ProtectedIssuanceEntry> protectedElements) {
        return protectedElements.stream().map(ProtectedIssuanceEntry::getVct).toList();
    }
}
