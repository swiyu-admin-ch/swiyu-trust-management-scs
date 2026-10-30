package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param hardDeleteDelaySeconds The frontend's view of {@code app.offboarding.hard-delete-delay}. The waiting period
 *     has to be named in the confirmation dialog before a deletion is requested, i.e. before a task with a deadline
 *     exists, so the configured duration itself is served here.
 */
@Schema(description = "Comprehensive application configuration for frontend client", name = "AppConfig")
public record AppConfigDto(
    @NotBlank String environment,
    @NotNull AuthConfigDto authConfig,
    @NotNull String version,
    @NotNull FeatureTogglesDto featureToggles,
    @Schema(description = "Waiting period between requesting and scheduling irreversible deletion, in seconds.")
    @NotNull
    Long hardDeleteDelaySeconds
) {}
