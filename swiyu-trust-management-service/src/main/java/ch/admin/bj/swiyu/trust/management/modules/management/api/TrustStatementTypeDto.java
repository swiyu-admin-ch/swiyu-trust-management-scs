package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustStatementType", enumAsRef = true)
public enum TrustStatementTypeDto {
    METADATA_V1,
    IDENTITY_V1,
    ISSUANCE_V1,
    VERIFICATION_V1,
}
