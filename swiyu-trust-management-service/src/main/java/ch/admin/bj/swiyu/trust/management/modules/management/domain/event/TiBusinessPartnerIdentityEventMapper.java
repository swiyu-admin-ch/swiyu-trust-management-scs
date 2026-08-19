package ch.admin.bj.swiyu.trust.management.modules.management.domain.event;

import ch.admin.bj.swiyu.messagetype.ti.BusinessPartnerIdentityStatus;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TiBusinessPartnerIdentityEventMapper {

    public static BusinessPartnerIdentityStatus toBusinessPartnerIdentityStatus(
        ch.admin.bj.swiyu.trust.management.modules.management.domain.BusinessPartnerIdentityStatus status
    ) {
        return switch (status) {
            case DEACTIVATED -> BusinessPartnerIdentityStatus.DEACTIVATED;
            case ACTIVE -> BusinessPartnerIdentityStatus.ACTIVE;
        };
    }
}
