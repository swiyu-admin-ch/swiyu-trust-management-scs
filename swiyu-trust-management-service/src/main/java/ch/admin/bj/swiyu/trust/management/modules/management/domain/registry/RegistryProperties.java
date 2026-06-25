package ch.admin.bj.swiyu.trust.management.modules.management.domain.registry;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "registry")
public record RegistryProperties(@NotNull URI trustRegistryDataUri) {}
