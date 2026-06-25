package ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import ch.admin.bj.swiyu.trust.client.core.business.b2b.api.StatusB2BApi;
import ch.admin.bj.swiyu.trust.client.core.business.b2b.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class StatusRegistryApiConfig {

    /**
     * has to match a client below spring.security.oauth2.client.registration
     */
    private static final String STATUS_REGISTRY_API = "status-registry-api";

    private final StatusRegistryProperties properties;

    @Bean
    public ApiClient statusRegistryApiClient(JeapOAuth2RestClientBuilderFactory factory) {
        var restClient = factory.createForClientRegistryId(STATUS_REGISTRY_API).build();
        return new ApiClient(restClient).setBasePath(String.valueOf(properties.writeBaseUrl()));
    }

    @Bean
    StatusB2BApi statusB2bApi(ApiClient coreBusinessServiceB2bApiClient) {
        return new StatusB2BApi(coreBusinessServiceB2bApiClient);
    }
}
