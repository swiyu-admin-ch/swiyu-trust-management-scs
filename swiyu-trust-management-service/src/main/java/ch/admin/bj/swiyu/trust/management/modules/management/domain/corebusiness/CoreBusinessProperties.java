package ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @param coreBusinessServiceInternalBaseUrl
 */
@Validated
@ConfigurationProperties(prefix = "core-business")
public record CoreBusinessProperties(@Valid @NotNull URL coreBusinessServiceInternalBaseUrl) {}
