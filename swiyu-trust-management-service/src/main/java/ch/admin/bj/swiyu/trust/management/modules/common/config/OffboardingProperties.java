package ch.admin.bj.swiyu.trust.management.modules.common.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

/**
 * Product configuration of the offboarding flow. Lives in {@code common} so every module - including the web layer,
 * which serves the waiting period to the frontend - may read it directly.
 *
 * @param hardDeleteDelay Time between the creation of a delete-business-partner task and the moment it becomes due
 *     for auto-approval, i.e. how long the deletion can still be called off. Any unit is allowed: {@code 5m} on the
 *     lower stages so e2e tests do not have to wait, {@code 2d} on PRD. {@link DurationUnit} does not restrict the
 *     unit, it only decides how a value without one is read - a bare {@code 2} means two days and not two
 *     milliseconds, which is Spring's default for {@link Duration}.
 */
@Validated
@ConfigurationProperties(prefix = "app.offboarding")
public record OffboardingProperties(@NotNull @DurationUnit(ChronoUnit.DAYS) Duration hardDeleteDelay) {}
