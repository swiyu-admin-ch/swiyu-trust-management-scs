package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TrustStatementType", enumAsRef = true)
public enum TrustStatementTypeDto {
    IDENTITY_V1,
    ISSUANCE_V1,
    VERIFICATION_V1,
    // Trust Protocol 2.0
    IDENTITY_V2,
    PROTECTED_VERIFICATION_AUTHORIZATION_V2,
    PROTECTED_ISSUANCE_AUTHORIZATION_V2,
    NON_COMPLIANCE_V2,
    PROTECTED_ISSUANCE_V2,
    PUBLIC_STATEMENT_VERIFICATION_QUERY_V2,
}
