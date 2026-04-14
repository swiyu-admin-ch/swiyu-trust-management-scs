package ch.admin.bj.swiyu.trust.management;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.data.web.config.*;
import org.springframework.transaction.annotation.*;

@SpringBootApplication
@Slf4j
@EnableTransactionManagement(order = Ordered.LOWEST_PRECEDENCE - 1) // for jeap-messaging-idempotence/outbox
@ConfigurationPropertiesScan
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class Application {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(Application.class, args).getEnvironment();
        log.info(
            """

            ----------------------------------------------------------------------------
            \t'{}' is running!\s
            \tProfile(s):         {}
            \tSwaggerUI:          http://localhost:{}/swagger-ui/index.html
            \tFrontend:           http://localhost:8401/
            \tFrontend (Bundled): http://localhost:{}/
            ----------------------------------------------------------------------------""",
            env.getProperty("spring.application.name"),
            env.getActiveProfiles(),
            env.getProperty("server.port"),
            env.getProperty("server.port")
        );
    }
}
