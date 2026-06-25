package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.didresolveradapter.DidResolverAdapter;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.SignedJWT;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DidService {

    private final DidResolverAdapter didResolverAdapter;

    /**
     * Verifies that a given signed JWT can be verified by a given DID
     *
     * @param kid       DID + Key Identifier of the public key
     * @param signedJWT Signed JWT
     * @return true if the public and private key match.
     */
    public boolean validateSignerMatchedKid(String kid, SignedJWT signedJWT) throws JOSEException {
        var jwk = didResolverAdapter.resolveKey(kid, Map.of());
        JWSVerifier verifier = switch (jwk) {
            case RSAKey rsaKey -> new RSASSAVerifier(rsaKey);
            case ECKey ecKey -> new ECDSAVerifier(ecKey);
            case OctetSequenceKey octKey -> new MACVerifier(octKey.toByteArray());
            default -> throw new IllegalArgumentException(
                "Key of unknown format %s.".formatted(jwk.getAlgorithm().getName())
            );
        };

        return signedJWT.verify(verifier);
    }
}
