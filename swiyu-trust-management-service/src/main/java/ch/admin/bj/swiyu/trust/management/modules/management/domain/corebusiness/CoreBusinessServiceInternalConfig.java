package ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustAddDidsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.TrustOnboardingSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.VcSchemaSubmissionApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.api.VqpsSubmissionInternalApi;
import ch.admin.bj.swiyu.trust.client.core.business.internal.invoker.ApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class CoreBusinessServiceInternalConfig {

    /**
     * Has to match a client below spring.security.oauth2.client.registration.
     */
    private static final String CORE_BUSINESS_SERVICE_REGISTRATION_ID = "core-business-service-internal"; // has to match a client below spring.security.oauth2.client.registration

    private final CoreBusinessProperties properties;

    @Bean
    public ApiClient coreBusinessServiceApiClient(JeapOAuth2RestClientBuilderFactory factory) {
        var restClient = factory.createForClientRegistryId(CORE_BUSINESS_SERVICE_REGISTRATION_ID).build();
        return new ApiClient(restClient).setBasePath(String.valueOf(properties.coreBusinessServiceInternalBaseUrl()));
    }

    @Bean
    public VcSchemaSubmissionApi vcSchemaSubmissionApi(ApiClient coreBusinessServiceApiClient) {
        return new VcSchemaSubmissionApi(coreBusinessServiceApiClient);
    }

    @Bean
    public TrustOnboardingSubmissionApi trustOnboardingSubmissionApi(ApiClient coreBusinessServiceApiClient) {
        return new TrustOnboardingSubmissionApi(coreBusinessServiceApiClient);
    }

    @Bean
    public RestTemplate documentClient(ClientHttpRequestFactory timeoutAwareRequestFactory) {
        return new RestTemplate(timeoutAwareRequestFactory);
    }

    @Bean
    TrustAddDidsSubmissionInternalApi trustAddDidsSubmissionInternalApi(ApiClient coreBusinessServiceApiClient) {
        return new TrustAddDidsSubmissionInternalApi(coreBusinessServiceApiClient);
    }

    @Bean
    VqpsSubmissionInternalApi vqpsSubmissionInternalApi(ApiClient coreBusinessServiceApiClient) {
        return new VqpsSubmissionInternalApi(coreBusinessServiceApiClient);
    }
}
