package ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @param businessPartnerId BusinessPartnerId of the government trust issuer and public transparency issuer
 */
@Validated
@ConfigurationProperties(prefix = "app.issuer.trust-root")
public record IssuerTrustRootProperties(@NotNull UUID businessPartnerId) {}
