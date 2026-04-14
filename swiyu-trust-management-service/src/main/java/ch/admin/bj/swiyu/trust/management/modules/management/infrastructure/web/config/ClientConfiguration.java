package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.config;

import jakarta.annotation.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.boot.http.client.*;
import org.springframework.context.annotation.*;
import org.springframework.http.client.*;

@Slf4j
@Configuration
@AllArgsConstructor
public class ClientConfiguration {

    private final HttpProperties httpProperties;

    @Bean
    public ClientHttpRequestFactory timeoutAwareRequestFactory(
        ClientHttpRequestFactoryBuilder<JdkClientHttpRequestFactory> requestFactoryBuilder
    ) {
        JdkClientHttpRequestFactory factory = requestFactoryBuilder.build();
        factory.setReadTimeout(httpProperties.readTimeout());
        log.debug("Setting http read timeout to : {}", httpProperties.readTimeout());
        return factory;
    }

    @PostConstruct
    public void init() {
        if (httpProperties.maxRedirects() != null) {
            System.setProperty("http.maxRedirects", String.valueOf(httpProperties.maxRedirects()));
            log.debug("http.maxRedirects set to: " + httpProperties.maxRedirects());
        } else {
            log.debug("http.maxRedirects not set. Using default.");
        }
    }
}
