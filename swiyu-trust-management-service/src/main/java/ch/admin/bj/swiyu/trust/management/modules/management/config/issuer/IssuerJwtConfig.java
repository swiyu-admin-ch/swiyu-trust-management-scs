package ch.admin.bj.swiyu.trust.management.modules.management.config.issuer;

import ch.admin.bj.swiyu.jwssignatureservice.JwsSignatureService;
import ch.admin.bj.swiyu.jwssignatureservice.factory.strategy.KeyStrategyException;
import ch.admin.bj.swiyu.trust.management.modules.common.did.DidUtil;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration which sets up the two required signers for the Trust Protocol 2.0.
 * (trustIssuer and publicTransparencyIssuer)
 * <p>
 * Both signers can be set up either as:
 * HSM         - The signing is done in an HSM
 * SoftwareKey - The signing is done via a software key provided to the application
 * Temporary   - The application creates its own, temporary, key. Cannot be used outside local development as key is not published in identifier registry.
 */
@Configuration
@RequiredArgsConstructor
public class IssuerJwtConfig {

    private final IssuerJwtProperties issuerJwtProperties;

    @Bean
    SignerContext publicTransparencyIssuer(JwsSignatureService jwsSignatureService)
        throws KeyStrategyException, JOSEException {
        return switch (issuerJwtProperties.publicTransparencyIssuer().type()) {
            case HSM -> {
                var properties = issuerJwtProperties.publicTransparencyIssuer().hsm();
                yield new SignerContext(
                    DidUtil.getDidFromKeyId(properties.didWithKeyFragment()),
                    properties.didWithKeyFragment(),
                    jwsSignatureService.createSigner(properties.getSignatureConfiguration())
                );
            }
            case SOFTWARE_KEY -> {
                var properties = issuerJwtProperties.publicTransparencyIssuer().software();
                yield new SignerContext(
                    DidUtil.getDidFromKeyId(properties.didWithKeyFragment()),
                    properties.didWithKeyFragment(),
                    jwsSignatureService.createSigner(properties.getSignatureConfiguration())
                );
            }
            case TEMPORARY -> {
                var publicTransparencyIssuerKey = new ECKeyGenerator(Curve.P_256).generate();
                var kid = "did:tdw:QmbBoyVLWetfXMKwsrtZcejKVKhMY5nVy138R7F9bQwxtw:localhost#key-" + UUID.randomUUID();
                yield new SignerContext(
                    DidUtil.getDidFromKeyId(kid),
                    kid,
                    new ECDSASigner(publicTransparencyIssuerKey)
                );
            }
        };
    }

    @Bean
    SignerContext trustIssuer(JwsSignatureService jwsSignatureService) throws KeyStrategyException, JOSEException {
        return switch (issuerJwtProperties.trustIssuer().type()) {
            case HSM -> {
                var properties = issuerJwtProperties.trustIssuer().hsm();
                yield new SignerContext(
                    DidUtil.getDidFromKeyId(properties.didWithKeyFragment()),
                    properties.didWithKeyFragment(),
                    jwsSignatureService.createSigner(properties.getSignatureConfiguration())
                );
            }
            case SOFTWARE_KEY -> {
                var properties = issuerJwtProperties.trustIssuer().software();
                yield new SignerContext(
                    DidUtil.getDidFromKeyId(properties.didWithKeyFragment()),
                    properties.didWithKeyFragment(),
                    jwsSignatureService.createSigner(properties.getSignatureConfiguration())
                );
            }
            case TEMPORARY -> {
                var trustIssuerKey = new ECKeyGenerator(Curve.P_256).generate();
                var kid = "did:tdw:QmbBoyVLWetfXMKwsrtZcejKVKhMY5nVy138R7F9bQwxtw:localhost#key-" + UUID.randomUUID();
                yield new SignerContext(DidUtil.getDidFromKeyId(kid), kid, new ECDSASigner(trustIssuerKey));
            }
        };
    }
}
