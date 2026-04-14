package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Comprehensive application configuration for frontend client", name = "AppConfig")
public record AppConfigDto(@NotBlank String environment, @NotNull AuthConfigDto authConfig, @NotNull String version) {}
