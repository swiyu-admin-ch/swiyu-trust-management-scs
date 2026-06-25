package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.TRUST_STATEMENT_ISSUANCE_V1;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the details of a trust statement of type TrustStatementIssuanceV1.
 * See <a href="https://github.com/admin-ch-ssi/specifications-to-publish/blob/feat/EID-4989/trust-protocol-v1.0.md">spec</a>
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class IssuanceV1Details extends TrustStatementDetails {

    @NotBlank
    private String canIssue;

    IssuanceV1Details() {
        super(TRUST_STATEMENT_ISSUANCE_V1);
    }

    public IssuanceV1Details(String canIssue) {
        this();
        this.canIssue = canIssue;
    }
}
