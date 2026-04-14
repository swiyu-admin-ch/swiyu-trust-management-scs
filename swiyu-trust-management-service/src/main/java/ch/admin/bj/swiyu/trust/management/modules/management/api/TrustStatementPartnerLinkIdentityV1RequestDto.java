package ch.admin.bj.swiyu.trust.management.modules.management.api;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(name = "TrustStatementPartnerLinkIdentityV1Request")
@NoArgsConstructor
public class TrustStatementPartnerLinkIdentityV1RequestDto extends TrustStatementPartnerLinkV1RequestDto {

    @NotEmpty
    @Schema(
        description = "All the names of the organization.",
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
    Map<LanguageDto, String> entityName;

    @NotNull
    @Schema(description = "Indicates that the subject is considered a goverment approved state actor.")
    Boolean isStateActor;

    @Schema(
        description = "(Optional) Array that contains objects with the property type which defines the type of registry identifier and value which define the identifier of the the given type.",
        example = """
        [
              {
                "type": "UID",
                "value": "CHE-000.000.000"
              },
              {
                "type": "LEI",
                "value": "0A1B2C3D4E5F6G7H8J9I"
              }
            ]
        """
    )
    List<RegistryIdDto> registryIds;

    public TrustStatementPartnerLinkIdentityV1RequestDto(
        String subject,
        Instant validFrom,
        Instant validUntil,
        Map<LanguageDto, String> entityName,
        Boolean isStateActor,
        List<RegistryIdDto> registryIds
    ) {
        super(subject, validFrom, validUntil);
        this.entityName = entityName;
        this.isStateActor = isStateActor;
        this.registryIds = registryIds;
    }

    @RequiredArgsConstructor
    @Schema(name = "IdentityV1Language", enumAsRef = true)
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

    public record RegistryIdDto(String type, String value) {}
}
