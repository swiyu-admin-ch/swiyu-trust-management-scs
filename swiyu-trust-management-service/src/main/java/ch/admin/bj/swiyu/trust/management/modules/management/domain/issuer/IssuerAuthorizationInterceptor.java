package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Intercepts the requests and mutates the body to a signed JWT. Required by Gov Trust Issuer Agent Management Service.
 */
@Slf4j
@RequiredArgsConstructor
public class IssuerAuthorizationInterceptor implements ClientHttpRequestInterceptor {

    private final ECKey jwtSigningKey;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
        throws IOException {
        log.trace("signing request body to {} ", request.getURI());
        var signedPayload = toSignedJwt(body);
        return execution.execute(request, signedPayload.getBytes());
    }

    private String toSignedJwt(byte[] body) {
        var header = new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(jwtSigningKey.getKeyID()).build();
        var claims = new JWTClaimsSet.Builder().claim("data", new String(body)).build();
        var jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new ECDSASigner(jwtSigningKey));
        } catch (JOSEException e) {
            throw new IllegalStateException("failed to sign request", e);
        }
        return jwt.serialize();
    }
}
