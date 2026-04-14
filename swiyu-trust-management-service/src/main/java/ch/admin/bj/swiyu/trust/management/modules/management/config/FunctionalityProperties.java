package ch.admin.bj.swiyu.trust.management.modules.management.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Properties for enabling or disabling certain functionalities of the application.
 * @param automaticApprovalEnabled If enabled incoming trust onboarding submissions will be automatically approved without manual BJ approval.
 */
@Validated
@ConfigurationProperties(prefix = "app.functionality")
public record FunctionalityProperties(@NotNull Boolean automaticApprovalEnabled) {}
