package ch.admin.bj.swiyu.trust.management.modules.ui.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * The Jira numbered feature toggles the frontend needs to know about, served straight from
 * {@code FeaturesProperties}. Named after the toggle and not after what it does, so it is found and removed together
 * with the backend flag when the story is rolled out.
 */
@Schema(description = "Feature Flags", name = "FeatureToggles")
public record FeatureTogglesDto(@NotNull Boolean EIDARTFE_1571_COMPLETE_BP_OFFBOARDING) {}
