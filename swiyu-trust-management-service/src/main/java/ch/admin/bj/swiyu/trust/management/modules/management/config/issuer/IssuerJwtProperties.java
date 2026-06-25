package ch.admin.bj.swiyu.trust.management.modules.management.config.issuer;

import ch.admin.bj.swiyu.jwssignatureservice.dto.HSMPropertiesDto;
import ch.admin.bj.swiyu.jwssignatureservice.dto.SignatureConfigurationDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties to set up the two required signers for the Trust Protocol 2.0.
 * (trustIssuer and publicTransparencyIssuer)
 * <p>
 * Both signers can be set up either as:
 * HSM         - The signing is done in an HSM
 * SoftwareKey - The signing is done via a software key provided to the application
 * Temporary   - The application creates its own, temporary, key. Cannot be used outside local development as key is not published in identifier registry.
 */
@Valid
@ConfigurationProperties(prefix = "app.issuer.jwt")
public record IssuerJwtProperties(@Valid KeyProperties trustIssuer, @Valid KeyProperties publicTransparencyIssuer) {
    @Valid
    public record KeyProperties(@NotNull JwtType type, SoftwareKeyProperties software, HsmProperties hsm) {
        public enum JwtType {
            TEMPORARY,
            SOFTWARE_KEY,
            HSM,
        }

        public record SoftwareKeyProperties(@NotBlank String didWithKeyFragment, @NotBlank String privateKey) {
            public @NotNull SignatureConfigurationDto getSignatureConfiguration() {
                return SignatureConfigurationDto.builder()
                    .keyManagementMethod("key")
                    .privateKey(this.privateKey())
                    .verificationMethod(this.didWithKeyFragment())
                    .build();
            }
        }

        public record HsmProperties(
            @NotBlank String didWithKeyFragment,
            @NotBlank String keyId,
            @NotNull String keyPin,
            @NotBlank String host,
            @NotBlank String port,
            @NotBlank String user,
            @NotNull String userPin,
            @NotBlank String password
        ) {
            public @NotNull SignatureConfigurationDto getSignatureConfiguration() {
                return SignatureConfigurationDto.builder()
                    .keyManagementMethod("securosys")
                    .hsm(
                        HSMPropertiesDto.builder()
                            .keyId(this.keyId())
                            .userPin(this.userPin())
                            .host(this.host())
                            .port(this.port())
                            .user(this.user())
                            .password(this.password())
                            .build()
                    )
                    .verificationMethod(this.didWithKeyFragment())
                    .build();
            }
        }
    }
}
