package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

/**
 * Represents the details of a trust statement of type TrustStatementIdentityV2.
 * See <a href="https://confluence.bit.admin.ch/spaces/EIDTEAM/pages/1374138607/Trust+Protocol+2.0#TrustProtocol2.0-IdentityTrustStatement(idTS)Identity-Trust-Statement">spec</a>
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class ProtectedVerificationAuthorizationV2Details extends TrustStatementDetails {

    @Size(min = 1)
    private List<@NotNull AuthorizableField> authorizedFields;

    ProtectedVerificationAuthorizationV2Details() {
        // Default constructor needed for JSON deserialization
        super(TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_VERIFICATION_AUTHORIZATION_V2);
        this.authorizedFields = new ArrayList<>();
    }

    public ProtectedVerificationAuthorizationV2Details(
        @Size(min = 1) List<@NotNull AuthorizableField> authorizedFields
    ) {
        this();
        this.authorizedFields = authorizedFields;
    }

    @RequiredArgsConstructor
    public enum AuthorizableField {
        AHV_NUMBER("personal_administrative_number");

        @Getter
        private final String jsonRepresentation;
    }
}
