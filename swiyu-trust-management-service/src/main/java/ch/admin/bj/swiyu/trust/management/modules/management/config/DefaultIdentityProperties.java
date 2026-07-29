package ch.admin.bj.swiyu.trust.management.modules.management.config;

import jakarta.validation.Valid;
import java.time.Period;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Valid
@ConfigurationProperties(prefix = "app.identity-defaults")
public record DefaultIdentityProperties(@Valid Period validity) {}
