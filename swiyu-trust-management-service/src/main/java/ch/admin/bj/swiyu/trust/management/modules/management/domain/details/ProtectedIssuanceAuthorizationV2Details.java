package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the details of a trust statement of type ProtectedIssuanceAuthorizationV2.
 * See <a href="https://confluence.bit.admin.ch/spaces/EIDTEAM/pages/1374138607/Trust+Protocol+2.0">spec</a>
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class ProtectedIssuanceAuthorizationV2Details extends TrustStatementDetails {

    @NotNull
    ProtectedIssuanceAuthorizationV2Details.ProtectedIssuanceAuthorization canIssue;

    ProtectedIssuanceAuthorizationV2Details() {
        // Default constructor needed for JSON deserialization
        super(TrustStatementPartnerLinkType.TRUST_STATEMENT_PROTECTED_ISSUANCE_AUTHORIZATION_V2);
    }

    public ProtectedIssuanceAuthorizationV2Details(ProtectedIssuanceAuthorization canIssue) {
        this();
        this.canIssue = canIssue;
    }

    @AllArgsConstructor
    public enum Language {
        DEFAULT(""),
        @Deprecated(forRemoval = true, since = "3.29.1") // Remove in EID-6303
        EN("en"),
        EN_CH("en-CH"),
        DE_CH("de-CH"),
        FR_CH("fr-CH"),
        IT_CH("it-CH"),
        RM_CH("rm-CH");

        @Getter
        private final String languageCode;
    }

    public record ProtectedIssuanceAuthorization(
        @NotBlank String vct,
        Map<Language, @NotBlank String> vctName,
        Map<Language, @NotBlank String> reason
    ) {}
}
