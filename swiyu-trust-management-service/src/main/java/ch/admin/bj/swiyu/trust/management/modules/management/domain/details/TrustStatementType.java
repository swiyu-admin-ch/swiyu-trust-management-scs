package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TrustStatementType {
    TRUST_STATEMENT_METADATA_V1("TrustStatementMetadataV1", List.of("ts_metadatav1_sd_jwt")),
    TRUST_STATEMENT_IDENTITY_V1("TrustStatementIdentityV1", List.of("ts_identity1_sd_jwt")),
    TRUST_STATEMENT_ISSUANCE_V1("TrustStatementIssuanceV1", List.of("ts_issuance1_sd_jwt")),
    TRUST_STATEMENT_VERIFICATION_V1("TrustStatementVerificationV1", List.of("ts_verification1_sd_jwt"));

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
     *  <a href="docs/issuer-metadata-json-configuration-example.json">docs/issuer-metadata-json-configuration-example.json</a>
     */
    private final List<String> metadataCredentialSupportedIds;
}
