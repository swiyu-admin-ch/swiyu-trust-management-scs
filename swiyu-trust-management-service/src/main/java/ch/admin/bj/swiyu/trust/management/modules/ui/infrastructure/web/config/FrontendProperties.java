package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(@NotBlank String environment, @NotNull AuthProperties auth) {
    public record AuthProperties(
        @NotBlank String issuer,
        @NotNull Boolean useSilentRefresh,
        @NotBlank String clientId,
        @NotBlank String scope,
        @NotBlank String responseType,
        @NotNull Boolean requireHttps
    ) {}
}
