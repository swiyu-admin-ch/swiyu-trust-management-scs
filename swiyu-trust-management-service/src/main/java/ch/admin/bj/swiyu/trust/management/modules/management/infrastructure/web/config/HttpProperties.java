package ch.admin.bj.swiyu.trust.management.modules.management.infrastructure.web.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param maxRedirects Configures the maximum number of HTTP redirects by setting the `http.maxRedirects` system property.
 *                     This configuration is necessary because Spring Boot, by default, uses `HttpURLConnection` as the underlying
 *                     HTTP client for requests and `HttpURLConnection` respects the `http.maxRedirects` system property to manage redirects.
 *                     <p>
 *                     Unlike properties such as `readTimeout` and `connectionTimeout`, which can be configured directly through
 *                     Spring's `application.yml`, the `http.maxRedirects` property must be set as a system property at the JVM level.
 *                     This is because `HttpURLConnection` does not expose a direct configuration property for controlling redirects.
 *                     <p>
 *                     If the property is not set, the default behavior of `HttpURLConnection` will allow up to 20 redirects.
 */
@Validated
@ConfigurationProperties(prefix = "http")
public record HttpProperties(Integer maxRedirects, Duration readTimeout) {
    public HttpProperties {
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(5);
        }
    }
}
