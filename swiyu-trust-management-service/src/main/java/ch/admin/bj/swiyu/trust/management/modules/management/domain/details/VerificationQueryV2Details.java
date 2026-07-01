package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents the details of a trust statement of type VerificationQueryPublicStatement.
 * See <a href="https://confluence.bit.admin.ch/spaces/EIDTEAM/pages/1374138607/Trust+Protocol+2.0#TrustProtocol2.0-IdentityTrustStatement(idTS)Identity-Trust-Statement">spec</a>
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Getter
public final class VerificationQueryV2Details extends TrustStatementDetails {

    @Valid
    private Map<Language, @NotBlank @Size(max = 50) String> purposeName;

    @Valid
    private Map<Language, @NotBlank @Size(max = 500) String> purposeDescription;

    @Valid
    @NotNull
    private VerificationRequestObject request;

    VerificationQueryV2Details() {
        super(TrustStatementPartnerLinkType.PUBLIC_STATEMENT_VERIFICATION_QUERY_V2);
    }

    public VerificationQueryV2Details(
        Map<Language, @NotBlank @Size(max = 50) String> purposeName,
        Map<Language, @NotBlank @Size(max = 500) String> purposeDescription,
        @Valid @NotNull VerificationRequestObject request
    ) {
        this();
        this.purposeName = purposeName;
        this.purposeDescription = purposeDescription;
        this.request = request;
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

    @Valid
    public record VerificationRequestObject(@NotBlank String type, @NotBlank String scope, @NotNull JsonNode query) {}
}
