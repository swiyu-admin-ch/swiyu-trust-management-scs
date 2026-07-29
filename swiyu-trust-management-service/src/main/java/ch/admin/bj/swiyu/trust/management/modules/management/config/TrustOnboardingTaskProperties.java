package ch.admin.bj.swiyu.trust.management.modules.management.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

/**
 * @param dueDatePeriod              Time reviewers have to act on a trust onboarding task before it is overdue.
 * @param rejectionEnforcementPeriod Time a partner has to resubmit a trust onboarding task after more information was requested, before it is automatically rejected.
 * @param rejectionEnforcementCheckCron Cron expression controlling how often tasks past their rejection-enforcement deadline are automatically rejected.
 */
@Validated
@ConfigurationProperties(prefix = "app.trust-onboarding-task")
public record TrustOnboardingTaskProperties(
    @NotNull @DurationUnit(ChronoUnit.DAYS) Duration dueDatePeriod,
    @NotNull @DurationUnit(ChronoUnit.DAYS) Duration rejectionEnforcementPeriod,
    @NotBlank String rejectionEnforcementCheckCron
) {}
