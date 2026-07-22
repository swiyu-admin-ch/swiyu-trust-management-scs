package ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness;

import ch.admin.bj.swiyu.trust.client.zas.sbn.api.UsnApi;
import ch.admin.bj.swiyu.trust.client.zas.sbn.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@ConfigurationPropertiesScan
public class ZasSbnConfiguration {

    private final ZasSbnProperties zasSbnProperties;

    @Bean
    public ApiClient zasSbnApiClient(
        RestClient.Builder restClientBuilder,
        ClientHttpRequestFactory timeoutAwareRequestFactory
    ) {
        var restClient = restClientBuilder.requestFactory(timeoutAwareRequestFactory).build();
        return new ApiClient(restClient).setBasePath(zasSbnProperties.baseUrl().toString());
    }

    @Bean
    public UsnApi usnApi(ApiClient zasSbnApiClient) {
        return new UsnApi(zasSbnApiClient);
    }
}
