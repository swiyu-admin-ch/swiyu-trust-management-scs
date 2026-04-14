package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementType.TRUST_STATEMENT_VERIFICATION_V1;

import jakarta.validation.constraints.NotBlank;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the details of a trust statement of type TrustStatementVerificationV1.
 * <p>
 * See <a href="https://github.com/admin-ch-ssi/specifications-to-publish/blob/feat/EID-4989/trust-protocol-v1.0.md">spec</a>
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public final class VerificationV1Details extends TrustStatementDetails {

    @NotBlank
    private String canVerify;

    VerificationV1Details() {
        super(TRUST_STATEMENT_VERIFICATION_V1);
    }

    public VerificationV1Details(String canVerify) {
        this();
        this.canVerify = canVerify;
    }
}
