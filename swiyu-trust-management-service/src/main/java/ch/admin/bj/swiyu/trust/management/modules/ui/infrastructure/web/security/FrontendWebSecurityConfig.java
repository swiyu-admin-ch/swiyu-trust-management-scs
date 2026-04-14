package ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.security;

import static org.springframework.http.HttpMethod.GET;

import ch.admin.bj.swiyu.trust.management.modules.ui.infrastructure.web.config.CorsProperties;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Note: Most of the security config comes via MvcSecurityConfiguration from JEAP.
 *
 * @see: ch.admin.bit.jeap.security.resource.configuration.MvcSecurityConfiguration
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class FrontendWebSecurityConfig {

    private static final String[] PUBLIC_STATIC_RESOURCES = {
        "/assets/**",
        "/media/**",
        "/robots.txt",
        "/*.ico",
        "/*.svg",
        "/*.woff",
        "/*.woff2",
        "/*.ttf",
        "/*.js",
        "/*.css",
        "/*.map",
    };

    private static final String[] DENIED_STATIC_RESOURCES = {
        // swagger-ui.js gibt es gar nicht, aber Security Scans nutzen das scheinbar als Angriffsvektor
        // was bei uns zu unnötigen LogAlerts führt.
        "/swagger-ui.js",
    };

    /**
     * Angular Routes.
     */
    private static final String[] PUBLIC_UI_ROUTES = { "/", "/index.html", "/ui/**" };

    private static final String[] PUBLIC_API = { "/ui-api/configuration/**" };

    private static final RequestMatcher[] PUBLIC_GET_ENDPOINTS = Stream.of(
        Stream.of(PUBLIC_STATIC_RESOURCES),
        Stream.of(PUBLIC_UI_ROUTES),
        Stream.of(PUBLIC_API)
    )
        .flatMap(s -> s)
        .map(s -> PathPatternRequestMatcher.withDefaults().matcher(GET, s))
        .toArray(PathPatternRequestMatcher[]::new);

    private static final RequestMatcher[] DENIED_GET_ENDPOINTS = Stream.of(Stream.of(DENIED_STATIC_RESOURCES))
        .flatMap(s -> s)
        .map(s -> PathPatternRequestMatcher.withDefaults().matcher(GET, s))
        .toArray(PathPatternRequestMatcher[]::new);

    private final CorsProperties corsProperties;

    /**
     * Zusätlziche Endpoints welche wir explizit verbieten, bspw. durch Erkenntnisse von Security Scans.
     */
    @Bean
    @Order(99)
    SecurityFilterChain deniedEndpointsFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(RequestMatchers.anyOf(DENIED_GET_ENDPOINTS))
            .authorizeHttpRequests(auth -> auth.anyRequest().denyAll());
        return http.build();
    }

    /**
     * Ergänzt die Security Chain aus JEAP's MvcSecurityConfiguration um Public Endpoints zuzulassen.
     */
    @Bean
    @Order(100)
    SecurityFilterChain publicEndpointsFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(RequestMatchers.anyOf(PUBLIC_GET_ENDPOINTS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
