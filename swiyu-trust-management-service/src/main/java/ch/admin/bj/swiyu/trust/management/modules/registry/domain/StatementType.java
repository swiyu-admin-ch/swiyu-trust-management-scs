package ch.admin.bj.swiyu.trust.management.modules.registry.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * This enum conveys the statements of Trust Protocol 2 and upward which are
 * persisted via the Statement entity.
 */
@RequiredArgsConstructor
public enum StatementType {
    IDENTITY_TRUST_STATEMENT_V2("IDENTITY_TRUST_STATEMENT_V2", "swiyu-identity-trust-statement+jwt"),
    VERIFICATION_QUERY_PUBLIC_STATEMENT_V2(
        "VERIFICATION_QUERY_PUBLIC_STATEMENT_V2",
        "swiyu-verification-query-public-statement+jwt"
    ),
    PROTECTED_VERIFICATION_AUTHORIZATION_TRUST_STATEMENT_V2(
        "PROTECTED_VERIFICATION_AUTHORIZATION_TRUST_STATEMENT_V2",
        "swiyu-protected-verification-authorization-trust-statement+jwt"
    ),
    PROTECTED_ISSUANCE_AUTHORIZATION_TRUST_STATEMENT_V2(
        "PROTECTED_ISSUANCE_AUTHORIZATION_TRUST_STATEMENT_V2",
        "swiyu-protected-issuance-authorization-trust-statement+jwt"
    ),
    PROTECTED_ISSUANCE_TRUST_LIST_STATEMENT_V2(
        "PROTECTED_ISSUANCE_TRUST_LIST_STATEMENT_V2",
        "swiyu-protected-issuance-trust-list-statement+jwt"
    ),
    NON_COMPLIANCE_TRUST_LIST_STATEMENT_V2(
        "NON_COMPLIANCE_TRUST_LIST_STATEMENT_V2",
        "swiyu-non-compliance-trust-list-statement+jwt"
    );

    private final String dbValue;

    @Getter
    private final String jwtTyp;

    @Override
    public String toString() {
        return dbValue;
    }
}
