package ch.admin.bj.swiyu.trust.management.modules.management.api;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
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
@Schema(name = "ProtectedIssuanceAuthorizationV2Request")
@NoArgsConstructor
public class ProtectedIssuanceAuthorizationV2RequestDto extends V1RequestDto {

    @NotNull
    @Schema(description = "The Business Partner ID.")
    UUID businessPartnerId;

    @Valid
    @NotNull
    @Schema(description = "The details of the issuance authorization.")
    ProtectedIssuanceAuthorizationDto canIssue;

    public ProtectedIssuanceAuthorizationV2RequestDto(
        UUID businessPartnerId,
        String subject,
        Instant validFrom,
        Instant validUntil,
        @NotNull ProtectedIssuanceAuthorizationDto canIssue
    ) {
        super(subject, validFrom, validUntil);
        this.businessPartnerId = businessPartnerId;
        this.canIssue = canIssue;
    }

    @Valid
    @Schema(name = "ProtectedIssuanceAuthorization")
    public record ProtectedIssuanceAuthorizationDto(
        @NotBlank String vct,
        @Valid @NotNull @ValidLocalizedMap Map<String, @NotBlank @Size(max = 40) String> vctName,
        @Valid @NotNull @ValidLocalizedMap Map<String, @NotBlank @Size(max = 1000) String> reason
    ) {}
}
