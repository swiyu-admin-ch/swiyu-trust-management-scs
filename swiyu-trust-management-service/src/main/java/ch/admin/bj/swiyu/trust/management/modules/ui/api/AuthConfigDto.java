package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Auth configuration for frontend", name = "AuthConfig")
public record AuthConfigDto(
    @NotBlank String issuer,
    @NotNull Boolean useSilentRefresh,
    @NotBlank String clientId,
    @NotBlank String scope,
    @NotBlank String responseType,
    @NotNull Boolean requireHttps
) {}
