package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TrustStatementPartnerLinkType {
    TRUST_STATEMENT_IDENTITY_V1("TrustStatementIdentityV1", List.of("ts_identity1_sd_jwt")),
    TRUST_STATEMENT_ISSUANCE_V1("TrustStatementIssuanceV1", List.of("ts_issuance1_sd_jwt")),
    TRUST_STATEMENT_VERIFICATION_V1("TrustStatementVerificationV1", List.of("ts_verification1_sd_jwt")),

    // Trust Protocol 2.0
    // Does not utilize the second parameter. Can be if we contract TP1.0 compatibility
    TRUST_STATEMENT_IDENTITY_V2("TrustStatementIdentityV2", null),
    TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2("TrustStatementVerificationV2", null),
    TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2("TrustStatementIssuanceV2", null),
    TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2("TrustListStatementNonComplianceV2", null),
    TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2("TrustListStatementProtectedIssuanceV2", null),
    PUBLIC_STATEMENT_VERIFICATION_QUERY_V2("PublicStatementVerificationQueryV2", null);

    /**
     * The type id of trust statement for this type as described in spec, e.g.
     * <a href="https://github.com/admin-ch-ssi/specifications-to-publish/blob/feat/EID-4989/trust-protocol-v1.0.md">trust protocol spec</a>
     */
    public final String vct;
    /**
     * The IDs of the trust statement VCs under which the gov trust issuer is configured to issue the VC (property
     * credential_configurations_supported in metadata.json).
     * <p>
     * An example of a metadata.json file can be found here:
     * <a href="docs/issuer-metadata-json-configuration-example.json">docs/issuer-metadata-json-configuration-example.json</a>
     */
    private final List<String> metadataCredentialSupportedIds;
}
