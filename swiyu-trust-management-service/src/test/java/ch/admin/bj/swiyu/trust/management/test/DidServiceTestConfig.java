package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.didresolveradapter.config.DidResolverAdapterConfiguration;
import ch.admin.bj.swiyu.didresolveradapter.config.DidResolverWebClientConfiguration;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.DidService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

@TestConfiguration
@Import({ DidService.class, DidResolverAdapterConfiguration.class, DidResolverWebClientConfiguration.class })
public class DidServiceTestConfig {

    @Bean
    @ConditionalOnMissingBean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
