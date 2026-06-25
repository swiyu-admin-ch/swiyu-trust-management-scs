package ch.admin.bj.swiyu.trust.management.modules.registry.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StatementType", enumAsRef = true)
public enum StatementTypeDto {
    TRUST_STATEMENT_IDENTITY_V2,
    TRUST_STATEMENT_VERIFICATION_AUTHORIZATION_V2,
    TRUST_STATEMENT_ISSUANCE_AUTHORIZATION_V2,
    TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2,
    TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2,
    PUBLIC_STATEMENT_VERIFICATION_QUERY_V2,
}
