package ch.admin.bj.swiyu.trust.management.modules.management.domain.issuer;

import ch.admin.bj.swiyu.trust.client.issuer.management.api.CredentialApi;
import ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.IssuerOid4VciApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@ConfigurationPropertiesScan
@Slf4j
public class IssuerConfiguration {

    private final IssuerProperties issuerClientsProperties;
    private final ClientHttpRequestFactory timeoutAwareRequestFactory;
    /**
     * Private key to sign requests to gov trust issuer and trust registry.
     */
    private ECKey jwtSigningKey;

    private static ClientHttpRequestInterceptor applyJsonContentTypeInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return execution.execute(request, body);
        };
    }

    @PostConstruct
    void init() {
        try {
            var ecKey = JWK.parseFromPEMEncodedObjects(issuerClientsProperties.signing().jwtSigningKey()).toECKey();
            this.jwtSigningKey = new ECKey.Builder(ecKey)
                .keyID(issuerClientsProperties.signing().jwtSigningKeyId())
                .build();
        } catch (JOSEException e) {
            throw new IllegalStateException("failed to parse pem formatted private key to JWK", e);
        }
    }

    @Bean
    public CredentialApi issuerManagementApi(RestClient.Builder restClientBuilder) {
        var client = new ch.admin.bj.swiyu.trust.client.issuer.management.invoker.ApiClient(
            restClientBuilder
                .requestFactory(timeoutAwareRequestFactory)
                .requestInterceptor(new IssuerAuthorizationInterceptor(jwtSigningKey))
                .build()
        );
        client.setBasePath(issuerClientsProperties.managementApiUrl().toString());
        return new CredentialApi(client);
    }

    @Bean
    public IssuerOid4VciApi issuerOid4vciApi(RestClient.Builder restClientBuilder) {
        var client = new ch.admin.bj.swiyu.trust.client.issuer.oid4vci.invoker.ApiClient(
            restClientBuilder
                .requestFactory(timeoutAwareRequestFactory)
                .requestInterceptor(applyJsonContentTypeInterceptor())
                .build()
        );
        client.setBasePath(issuerClientsProperties.oid4vciApiUrl().toString());
        return new IssuerOid4VciApi(client);
    }

    @Bean
    public ch.admin.bj.swiyu.trust.client.issuer.management.api.ActuatorApi issuerManagementActuatorApi(
        RestClient.Builder restClientBuilder
    ) {
        var client = new ch.admin.bj.swiyu.trust.client.issuer.management.invoker.ApiClient(
            restClientBuilder.requestFactory(timeoutAwareRequestFactory).build()
        );
        client.setBasePath(issuerClientsProperties.managementApiUrl().toString());
        return new ch.admin.bj.swiyu.trust.client.issuer.management.api.ActuatorApi(client);
    }

    @Bean
    public ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.ActuatorApi issuerOid4vciActuatorApi(
        RestClient.Builder restClientBuilder
    ) {
        var client = new ch.admin.bj.swiyu.trust.client.issuer.oid4vci.invoker.ApiClient(
            restClientBuilder.requestFactory(timeoutAwareRequestFactory).build()
        );
        client.setBasePath(issuerClientsProperties.oid4vciApiUrl().toString());
        return new ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.ActuatorApi(client);
    }

    @Bean
    @ConditionalOnProperty(name = "issuer.mockClient", havingValue = "false", matchIfMissing = true)
    public IssuerClient issuerClient(
        CredentialApi issuerManagementApi,
        IssuerOid4VciApi issuerOid4VciAPI,
        ObjectMapper objectMapper,
        IssuerProperties issuerProperties
    ) {
        return new DefaultIssuerClient(issuerManagementApi, issuerOid4VciAPI, objectMapper, issuerProperties);
    }

    @ConditionalOnProperty(name = "issuer.mockClient", havingValue = "true")
    @Bean
    public IssuerClient mockIssuerClient(IssuerProperties issuerProperties) {
        log.warn("Using mock issuer client. This should only be used for local testing purposes.");
        return new MockIssuerClient(issuerProperties);
    }
}
