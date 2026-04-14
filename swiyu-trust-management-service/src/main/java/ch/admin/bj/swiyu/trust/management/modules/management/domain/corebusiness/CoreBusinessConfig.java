package ch.admin.bj.swiyu.trust.management.modules.management.domain.corebusiness;

import ch.admin.bit.jeap.security.restclient.*;
import ch.admin.bj.swiyu.trust.client.core.business.api.*;
import ch.admin.bj.swiyu.trust.client.core.business.invoker.*;
import lombok.*;
import org.springframework.context.annotation.*;
import org.springframework.http.client.*;
import org.springframework.web.client.*;

@Configuration
@RequiredArgsConstructor
public class CoreBusinessConfig {

    private static final String coreBusinessServiceRegistrationId = "core-business-service"; // has to match a client below spring.security.oauth2.client.registration

    private final CoreBusinessProperties properties;

    @Bean
    public ApiClient coreBusinessServiceApiClient(JeapOAuth2RestClientBuilderFactory factory) {
        var restClient = factory.createForClientRegistryId(coreBusinessServiceRegistrationId).build();
        return new ApiClient(restClient).setBasePath(String.valueOf(properties.coreBusinessServiceBaseUrl()));
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
    TrustAddDidsSubmissionInternalApi trustAddDidsSubmissionInternalApi(ApiClient coreBusinessServiceApiClient) {
        return new TrustAddDidsSubmissionInternalApi(coreBusinessServiceApiClient);
    }

    @Bean
    public RestTemplate documentClient(ClientHttpRequestFactory timeoutAwareRequestFactory) {
        return new RestTemplate(timeoutAwareRequestFactory);
    }
}
