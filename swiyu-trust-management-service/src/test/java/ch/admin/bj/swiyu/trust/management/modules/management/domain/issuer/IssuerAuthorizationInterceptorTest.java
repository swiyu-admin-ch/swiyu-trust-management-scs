package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import static ch.admin.bj.swiyu.trust.management.test.IssuerTestData.jwtSigningKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.text.ParseException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

class IssuerAuthorizationInterceptorTest {

    IssuerAuthorizationInterceptor interceptor = new IssuerAuthorizationInterceptor(jwtSigningKey());

    @Test
    void intercept() throws IOException, ParseException {
        // GIVEN
        var body = "{hello: 'world'}";
        var execution = mock(ClientHttpRequestExecution.class);
        var request = mock(HttpRequest.class);
        var response = mock(ClientHttpResponse.class);
        when(execution.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(response);
        // WHEN
        try (var ignored = interceptor.intercept(request, body.getBytes(), execution)) {
            // THEN
            var captor = ArgumentCaptor.forClass(byte[].class);
            verify(execution).execute(eq(request), captor.capture());
            byte[] signedPayload = captor.getValue();
            var jwt = SignedJWT.parse(new String(signedPayload));
            assertThat(jwt.getHeader().getKeyID()).isEqualTo("testkey");
            assertThat(jwt.getJWTClaimsSet().getStringClaim("data")).isEqualTo(body);
        }
    }
}
