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
        @JsonSubTypes.Type(value = MetadataV1Details.class, name = "TRUST_STATEMENT_METADATA_V1"),
        @JsonSubTypes.Type(value = IdentityV1Details.class, name = "TRUST_STATEMENT_IDENTITY_V1"),
        @JsonSubTypes.Type(value = IssuanceV1Details.class, name = "TRUST_STATEMENT_ISSUANCE_V1"),
        @JsonSubTypes.Type(value = VerificationV1Details.class, name = "TRUST_STATEMENT_VERIFICATION_V1"),
    }
)
@EqualsAndHashCode
@Getter
public abstract sealed class TrustStatementDetails
    permits MetadataV1Details, VerificationV1Details, IdentityV1Details, IssuanceV1Details
{

    private final TrustStatementType type;

    protected TrustStatementDetails(TrustStatementType type) {
        this.type = type;
    }
}
