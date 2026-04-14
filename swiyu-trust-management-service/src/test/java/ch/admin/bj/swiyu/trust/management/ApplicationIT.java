package ch.admin.bj.swiyu.trust.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
class ApplicationIT {

    @LocalServerPort
    int port;

    @MockitoBean
    ch.admin.bj.swiyu.trust.client.issuer.management.api.ActuatorApi issuerManagementActuatorApi;

    @MockitoBean
    ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.ActuatorApi issuerOid4vciActuatorApi;

    private static Map<String, Object> getHealthResponse(Status statusValue) {
        return Map.of("status", statusValue.getCode());
    }

    @BeforeEach
    void setUp() {
        when(issuerManagementActuatorApi.health()).thenReturn(getHealthResponse(Status.UP));
        when(issuerOid4vciActuatorApi.health()).thenReturn(getHealthResponse(Status.UP));
    }

    @Test
    void testHealth() throws URISyntaxException, IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var url = "http://localhost:%s/actuator/health".formatted(port);
            var request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}");
        }
    }

    @Test
    void testHealthReadiness() throws URISyntaxException, IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var url = "http://localhost:%s/actuator/health/readiness".formatted(port);
            var request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
        }
    }

    @Test
    void testApiSecured() throws URISyntaxException, IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var url = "http://localhost:%s/api/v1/trust-statement-partner-links/".formatted(port);
            var jwtToken = createJwtToken();
            var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(401);
        }
    }

    @Test
    void testApiSecured_noAuthenticationHeader() throws URISyntaxException, IOException, InterruptedException {
        try (var client = HttpClient.newHttpClient()) {
            var url = "http://localhost:%s/api/v1/trust-statement-partner-links/".formatted(port);
            var request = HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(401);
        }
    }

    @Test
    void testHttpMaxRedirectsSystemPropertyIsSet() {
        var maxRedirects = System.getProperty("http.maxRedirects");
        assertEquals("5", maxRedirects);
    }

    private String createJwtToken() {
        try {
            // Generate a secure random key
            var sharedSecret = new byte[32];
            new java.security.SecureRandom().nextBytes(sharedSecret);

            // Create HMAC signer
            JWSSigner signer = new MACSigner(sharedSecret);

            // Prepare JWT with claims set
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3_600_000)) // 1 hour expiration
                .build();

            SignedJWT signedJWT = new SignedJWT(new com.nimbusds.jose.JWSHeader(JWSAlgorithm.HS256), claimsSet);

            // Apply the HMAC protection
            signedJWT.sign(signer);

            // Serialize to compact form, produces something like
            // eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiaXNzIjoieW91ci1pc3N1ZXIiLCJhdWQiOiJ5b3VyLWF1ZGllbmNlIiwiaWF0IjoxNjE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
