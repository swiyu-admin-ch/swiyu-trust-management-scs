package ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the ZAS (Zentrale Ausgleichsstelle) SBN API to request information concerning the AHV identificator
 * @param baseUrl
 */
@Validated
@ConfigurationProperties(prefix = "zas-sbn")
public record ZasSbnProperties(@Valid @NotNull URL baseUrl) {}
