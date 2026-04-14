package ch.admin.bj.swiyu.trust.management.modules.management.domain.registry;

import ch.admin.bj.swiyu.trust.management.modules.common.registry.VcSchemaUrlValidator;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConfigurationPropertiesScan
public class RegistryConfiguration {

    private final RestClient restClient;
    private final VcSchemaUrlValidator vcSchemaUrlValidator;

    public RegistryConfiguration(
        ClientHttpRequestFactory timeoutAwareRequestFactory,
        RestClient.Builder restClientBuilder,
        VcSchemaUrlValidator vcSchemaUrlValidator
    ) {
        this.restClient = restClientBuilder.requestFactory(timeoutAwareRequestFactory).build();
        this.vcSchemaUrlValidator = vcSchemaUrlValidator;
    }

    @Bean
    public TrustRegistryClient trustRegistryClient() {
        return new TrustRegistryClient(restClient, vcSchemaUrlValidator);
    }
}
