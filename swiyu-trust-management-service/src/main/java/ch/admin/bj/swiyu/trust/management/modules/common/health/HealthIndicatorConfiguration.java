package ch.admin.bj.swiyu.trust.management.modules.common.health;

import static org.springframework.boot.actuate.health.Status.UP;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HealthIndicatorConfiguration {

    private final ch.admin.bj.swiyu.trust.client.issuer.management.api.ActuatorApi issuerManagementActuatorApi;
    private final ch.admin.bj.swiyu.trust.client.issuer.oid4vci.api.ActuatorApi issuerOid4vciActuatorApi;

    @Bean
    public HealthIndicator issuerManagementHealthIndicator() {
        return () -> {
            try {
                log.debug("checking health of issuer management service...");
                var health = issuerManagementActuatorApi.health();
                validateIsUp(health);
                return Health.up().build();
            } catch (Exception e) {
                log.error("health check failed for gov trust issuer management", e);
                return Health.down().withException(e).build();
            }
        };
    }

    @Bean
    public HealthIndicator issuerOid4vciHealthIndicator() {
        return () -> {
            try {
                log.debug("checking health of issuer oid4vci service ...");
                var health = issuerOid4vciActuatorApi.health();
                validateIsUp(health);
                return Health.up().build();
            } catch (Exception e) {
                log.error("health check failed for gov trust issuer oid4vci", e);
                return Health.down().withException(e).build();
            }
        };
    }

    private static void validateIsUp(Object health) {
        if (!isUp(health)) {
            throw new IllegalStateException("health endpoint of service did not response with status UP");
        }
    }

    private static boolean isUp(Object health) {
        return health instanceof Map && ((Map<?, ?>) health).get("status").equals(UP.getCode());
    }
}
