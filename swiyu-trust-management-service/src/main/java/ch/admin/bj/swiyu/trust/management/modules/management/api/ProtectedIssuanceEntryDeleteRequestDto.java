package ch.admin.bj.swiyu.trust.management.modules.management.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "ProtectedIssuanceEntryDeleteRequest")
public record ProtectedIssuanceEntryDeleteRequestDto(@NotBlank String vct) {}
