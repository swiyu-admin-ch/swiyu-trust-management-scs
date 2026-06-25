package ch.admin.bj.swiyu.trust.management.modules.common.registry;

import static ch.admin.bj.swiyu.trust.management.modules.common.path.PathSupport.join;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "registry")
public record RegistryProperties(@NotNull URI trustRegistryDataUri, @NotBlank String trustRegistryVcSchemaEndpoint) {
    public URI getVcSchemaBaseUrl() {
        return join(trustRegistryDataUri, trustRegistryVcSchemaEndpoint);
    }
}
