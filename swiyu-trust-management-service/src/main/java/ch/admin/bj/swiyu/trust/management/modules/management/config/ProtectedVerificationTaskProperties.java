package ch.admin.bj.swiyu.trust.management.modules.management.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

/**
 * @param dueDatePeriod Time reviewers have to act on a protected verification request task before it is overdue.
 */
@Validated
@ConfigurationProperties(prefix = "app.protected-verification-task")
public record ProtectedVerificationTaskProperties(@NotNull @DurationUnit(ChronoUnit.DAYS) Duration dueDatePeriod) {}
