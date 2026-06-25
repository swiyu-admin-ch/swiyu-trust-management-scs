package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustRegistryStatus", enumAsRef = true)
public enum TrustRegistryStatusDto {
    UNKNOWN,
    OTHER,
    ACTIVE,
    INACTIVE;

    public boolean isInactive() {
        return INACTIVE.equals(this);
    }
}
