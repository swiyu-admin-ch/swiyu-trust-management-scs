package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@JsonAutoDetect(
    // serialize only fields to json, no getters/setters
    fieldVisibility = ANY,
    setterVisibility = NONE,
    getterVisibility = NONE,
    isGetterVisibility = NONE,
    creatorVisibility = NONE
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, visible = true, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = IdentityV1Details.class, name = "TRUST_STATEMENT_IDENTITY_V1"),
        @JsonSubTypes.Type(value = IdentityV2Details.class, name = "TRUST_STATEMENT_IDENTITY_V2"),
        @JsonSubTypes.Type(value = IssuanceV1Details.class, name = "TRUST_STATEMENT_ISSUANCE_V1"),
        @JsonSubTypes.Type(value = VerificationV1Details.class, name = "TRUST_STATEMENT_VERIFICATION_V1"),
        @JsonSubTypes.Type(
            value = ProtectedVerificationAuthorizationV2Details.class,
            name = "TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2"
        ),
        @JsonSubTypes.Type(
            value = ProtectedIssuanceAuthorizationV2Details.class,
            name = "TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2"
        ),
        @JsonSubTypes.Type(value = NonComplianceV2Details.class, name = "TRUST_LIST_STATEMENT_NON_COMPLIANCE_V2"),
        @JsonSubTypes.Type(
            value = ProtectedIssuanceV2Details.class,
            name = "TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2"
        ),
        @JsonSubTypes.Type(value = VerificationQueryV2Details.class, name = "PUBLIC_STATEMENT_VERIFICATION_QUERY_V2"),
    }
)
@EqualsAndHashCode
@Getter
public abstract sealed class TrustStatementDetails
    permits
        VerificationV1Details,
        IdentityV1Details,
        IdentityV2Details,
        IssuanceV1Details,
        ProtectedVerificationAuthorizationV2Details,
        ProtectedIssuanceAuthorizationV2Details,
        NonComplianceV2Details,
        ProtectedIssuanceV2Details,
        VerificationQueryV2Details
{

    private final TrustStatementPartnerLinkType type;

    protected TrustStatementDetails(TrustStatementPartnerLinkType type) {
        this.type = type;
    }
}
