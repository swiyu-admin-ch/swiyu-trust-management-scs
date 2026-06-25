package ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @param writeBaseUrl B2B Status Registry API
 */
@Validated
@ConfigurationProperties(prefix = "status-registry")
public record StatusRegistryProperties(@Valid @NotNull URL writeBaseUrl) {}
