package ch.admin.bj.swiyu.trust.management.modules.management.api;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(name = "VerificationQueryV2Request")
@NoArgsConstructor
public class VerificationQueryV2RequestDto extends V1RequestDto {

    @NotNull
    @Schema(description = "The Business Partner ID.")
    UUID businessPartnerId;

    @Valid
    @NotNull
    @ValidLocalizedMap
    @Schema(description = "Name of the purpose for this verification.")
    Map<String, @NotBlank @Size(max = 40) String> purposeName;

    @Valid
    @NotNull
    @ValidLocalizedMap
    @Schema(description = "Description of the purpose.")
    private Map<String, @NotBlank @Size(max = 1000) String> purposeDescription;

    @Valid
    @NotNull
    private VerificationQueryV2RequestDto.VerificationRequestObjectDto request;

    public VerificationQueryV2RequestDto(
        UUID businessPartnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        Map<String, String> purposeName,
        Map<String, String> purposeDescription,
        VerificationRequestObjectDto request
    ) {
        super(subject, validFrom, validUntil);
        this.businessPartnerId = businessPartnerId;
        this.purposeName = purposeName;
        this.purposeDescription = purposeDescription;
        this.request = request;
    }

    @Valid
    @Schema(name = "VerificationRequestObject")
    public record VerificationRequestObjectDto(
        @NotBlank @Schema(example = "DCQL") String type,
        @NotBlank String scope,
        @NotNull @Schema(
            example = """
            {
                "credentials": [
                  {
                    "id": "some_identity_credential",
                    "format": "dc+sd-jwt",
                    "meta": {
                      "vct_values": [ "https://credentials.example.com/identity_credential" ]
                    },
                    "claims": [
                        {"path": ["last_name"]},
                        {"path": ["first_name"]}
                    ]
                  }
                ]
              }"""
        ) JsonNode query
    ) {}
}
