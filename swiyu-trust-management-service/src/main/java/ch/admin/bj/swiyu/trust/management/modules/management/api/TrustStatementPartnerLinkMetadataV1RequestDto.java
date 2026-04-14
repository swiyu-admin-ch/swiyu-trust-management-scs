package ch.admin.bj.swiyu.trust.management.modules.management.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(name = "TrustStatementPartnerLinkMetadataV1Request")
@NoArgsConstructor
public class TrustStatementPartnerLinkMetadataV1RequestDto extends TrustStatementPartnerLinkV1RequestDto {

    @NotEmpty
    @Schema(
        description = "All the names of the organization. The orgName selected in preferredLanguage must exist",
        example = """
        {
                "en": "Example Organization",
                "de-CH": "Beispielorganisation",
                "fr-CH": "Exemple d'organisation",
                "it-CH": "Organizzazione di esempio",
                "rm-CH": "organisaziun exemplarica"
        }
        """
    )
    Map<LanguageDto, String> orgName;

    @Schema(
        description = "All URIs to the logo of the organization",
        example = """
                {
                        "en": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFwAAABcCAMAAADUMSJqAAAAGFBMVEX/AAD/////w8P/39//rKz/4+P/v7//6+th/ykyAAAAb0lEQVRoge3YQQrAIAwFUWtMvP+Nu250EcQUamfW8pb5YClERERbaiqPtG3E6+Wq4ODg4ODgb+LmcVuEWh2y7vFu46vIrqqHomkAl1VcwMHBwU/EU09u6ljM2jZzs767/uDg4ODgZ+Cpn5ZERPTrbtr5CF36/dVBAAAAAElFTkSuQmCC",
                        "de-CH": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFwAAABcCAMAAADUMSJqAAAAGFBMVEX/AAD/////w8P/39//rKz/4+P/v7//6+th/ykyAAAAb0lEQVRoge3YQQrAIAwFUWtMvP+Nu250EcQUamfW8pb5YClERERbaiqPtG3E6+Wq4ODg4ODgb+LmcVuEWh2y7vFu46vIrqqHomkAl1VcwMHBwU/EU09u6ljM2jZzs767/uDg4ODgZ+Cpn5ZERPTrbtr5CF36/dVBAAAAAElFTkSuQmCC"
                }
        """
    )
    Map<LanguageDto, String> logoUri;

    @NotNull
    @Schema(
        description = "the preferred language for the trust statement. Must exist in orgName and logoUri",
        example = "de-CH",
        defaultValue = "de-CH"
    )
    LanguageDto preferredLanguage;

    public TrustStatementPartnerLinkMetadataV1RequestDto(
        String subject,
        Instant validFrom,
        Instant validUntil,
        Map<LanguageDto, String> orgName,
        Map<LanguageDto, String> logoUri,
        LanguageDto preferredLanguage
    ) {
        super(subject, validFrom, validUntil);
        this.orgName = orgName;
        this.logoUri = logoUri;
        this.preferredLanguage = preferredLanguage;
    }

    @RequiredArgsConstructor
    @Schema(name = "MetadataV1Language", enumAsRef = true)
    public enum LanguageDto {
        EN("en"),
        DE_CH("de-CH"),
        FR_CH("fr-CH"),
        IT_CH("it-CH"),
        RM_CH("rm-CH");

        private final String value;

        @JsonValue
        @Override
        public String toString() {
            return value;
        }
    }
}
