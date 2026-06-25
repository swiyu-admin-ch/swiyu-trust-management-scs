package ch.admin.bj.swiyu.trust.management.modules.management.api;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(name = "IdentityV2Request")
@NoArgsConstructor
public class IdentityV2RequestDto extends V1RequestDto {

    @NotNull
    @Schema(description = "The Business Partner ID.")
    UUID businessPartnerId;

    @Valid
    @NotNull
    @ValidLocalizedMap
    @Schema(description = "All the names of the organization.")
    Map<String, @NotBlank String> entityName;

    @NotNull
    @Schema(
        description = "Indicates that the subject is considered a goverment approved state actor.",
        example = "false"
    )
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

    public IdentityV2RequestDto(
        UUID businessPartnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        Map<String, String> entityName,
        Boolean isStateActor,
        List<RegistryIdDto> registryIds
    ) {
        super(subject, validFrom, validUntil);
        this.businessPartnerId = businessPartnerId;
        this.entityName = entityName;
        this.isStateActor = isStateActor;
        this.registryIds = registryIds;
    }

    public record RegistryIdDto(String type, String value) {}
}
