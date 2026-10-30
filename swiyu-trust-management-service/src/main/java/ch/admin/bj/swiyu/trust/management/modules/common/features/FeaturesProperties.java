package ch.admin.bj.swiyu.trust.management.modules.common.features;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Jira numbered feature toggles. They mark work in progress, are removed again once the corresponding story is rolled
 * out, and must be listed on <a href="https://confluence.bit.admin.ch/spaces/EIDTGM/pages/982856284/Feature+Flags">
 * Feature Flags</a>. Same mechanism as in swiyu-core-business-service and swiyu-ecosystem-portal-scs.
 *
 * <p>This is distinct from {@link ch.admin.bj.swiyu.trust.management.modules.management.config.FunctionalityProperties},
 * whose flags are named after what they do and stay part of the product.
 *
 * <p>Lives in {@code common} so every module - including the web layer - may read it directly.
 */
@Slf4j
@Data
@Validated
@ConfigurationProperties(prefix = "features")
public final class FeaturesProperties {

    @NotNull
    private Boolean eidartfe1571CompleteBpOffboarding;

    @PostConstruct
    public void logFeatureFlags() {
        log.info(
            """
            Following features are configured:
              EIDARTFE_1571_COMPLETE_BP_OFFBOARDING:{}
            """,
            eidartfe1571CompleteBpOffboarding
        );
    }
}
