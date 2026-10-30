package ch.admin.bj.swiyu.trust.management.modules.management.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties for enabling or disabling certain functionalities of the application. Functionalities are named after
 * what they do and stay part of the product. This is distinct from the Jira numbered flags in
 * {@link ch.admin.bj.swiyu.trust.management.modules.common.features.FeaturesProperties}, which mark work in progress
 * and are removed once the corresponding story is rolled out.
 *
 * @param automaticApprovalEnabled If enabled incoming trust onboarding submissions will be automatically approved without manual BJ approval.
 */
@Validated
@ConfigurationProperties(prefix = "app.functionality")
public record FunctionalityProperties(@NotNull Boolean automaticApprovalEnabled) {}
