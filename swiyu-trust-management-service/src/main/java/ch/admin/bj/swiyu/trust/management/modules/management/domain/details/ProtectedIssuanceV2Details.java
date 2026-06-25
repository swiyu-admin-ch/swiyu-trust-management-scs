package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the details of a trust statement of type Protected Issuance Trust List Statement.
 * See <a href="https://confluence.bit.admin.ch/spaces/EIDTEAM/pages/1374138607/Trust+Protocol+2.0#TrustProtocol2.0-IdentityTrustStatement(idTS)Identity-Trust-Statement">spec</a>
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class ProtectedIssuanceV2Details extends TrustStatementDetails {

    private List<@NotBlank String> vctValues;

    ProtectedIssuanceV2Details() {
        // Default constructor needed for JSON deserialization
        super(TrustStatementPartnerLinkType.TRUST_LIST_STATEMENT_PROTECTED_ISSUANCE_V2);
        this.vctValues = new ArrayList<>();
    }

    public ProtectedIssuanceV2Details(List<String> vctValues) {
        this();
        this.vctValues = vctValues;
    }
}
