package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Valid
@Schema(name = "TrustStatementPartnerLinkV1Request")
@Getter
@EqualsAndHashCode
@NoArgsConstructor
public abstract class V1RequestDto {

    @NotBlank
    @Schema(
        description = "the subject of the trust statement, e.g. the issuer or verifiers did",
        example = "did:tdw:DEADBEEF0000000000000000000000000000000000000000000000000000000000000000000000000000000000000:identifier-data-service-d.bit.admin.ch:api:v1:did:00000000-0000-0000-0000-000000000000"
    )
    String subject;

    @NotNull
    @Schema(description = "the valid from date", example = "2026-01-01T00:00:00Z")
    Instant validFrom;

    @NotNull
    @Schema(description = "the valid to date", example = "2026-12-01T00:00:00Z")
    Instant validUntil;

    protected V1RequestDto(String subject, Instant validFrom, Instant validUntil) {
        this.subject = subject;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }
}
