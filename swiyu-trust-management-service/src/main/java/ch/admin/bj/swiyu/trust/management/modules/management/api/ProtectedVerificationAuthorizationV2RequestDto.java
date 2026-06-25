package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode(callSuper = true)
@Schema(name = "ProtectedVerificationAuthorizationV2Request")
@NoArgsConstructor
public class ProtectedVerificationAuthorizationV2RequestDto extends V1RequestDto {

    @NotNull
    @Schema(description = "The Business Partner ID.")
    UUID businessPartnerId;

    @Valid
    @NotNull
    @Size(min = 1)
    @Schema(description = "Array that contains the field the subject should be able to validate.")
    List<@NotNull AuthorizableFieldDto> authorizedFields;

    public ProtectedVerificationAuthorizationV2RequestDto(
        UUID businessPartnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        List<AuthorizableFieldDto> authorizedFields
    ) {
        super(subject, validFrom, validUntil);
        this.authorizedFields = authorizedFields;
        this.businessPartnerId = businessPartnerId;
    }

    @Schema(name = "AuthorizableField", enumAsRef = true)
    public enum AuthorizableFieldDto {
        AHV_NUMBER,
    }
}
