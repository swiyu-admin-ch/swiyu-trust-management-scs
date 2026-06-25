package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URL;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "issuer")
public record IssuerProperties(
    Boolean mockClient,
    @NotNull URL managementApiUrl,
    @NotNull URL oid4vciApiUrl,
    @NotBlank String statusListUri,
    @NotNull SigningProperties signing
) {
    @Validated
    public record SigningProperties(@NotBlank String jwtSigningKey, @NotBlank String jwtSigningKeyId) {}
}
