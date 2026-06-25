package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Schema(name = "ProtectedIssuanceEntry")
public record ProtectedIssuanceEntryDto(
    @NotEmpty UUID id,

    @NotBlank String vct,

    @NotNull @Schema(example = "2026-01-01T00:00:00Z") Instant protectedAt
) {}
