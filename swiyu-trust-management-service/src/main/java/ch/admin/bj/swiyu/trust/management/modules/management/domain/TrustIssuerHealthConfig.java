package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.didresolveradapter.DidResolverException;
import ch.admin.bj.swiyu.trust.management.modules.management.config.issuer.SignerContext;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClientException;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableScheduling
@ConditionalOnExpression("'${app.issuer.jwt.trust-issuer.type}' != 'temporary'")
public class TrustIssuerHealthConfig {

    private final SignerContext trustIssuer;
    private final DidService didService;
    private final JwtStatementDomainService jwtStatementDomainService;

    private KeyState trustIssuerKeyState = KeyState.UNKNOWN;

    @Bean
    public HealthIndicator trustIssuerKeyConfigHealthIndicator() {
        return () -> {
            if (this.trustIssuerKeyState == KeyState.OK) {
                return Health.up().build();
            }
            return Health.down().withDetail("trustIssuerKeyState", this.trustIssuerKeyState).build();
        };
    }

    @VisibleForTesting
    public KeyState checkTrustIssuerKeyValid() {
        try {
            if (
                !didService.validateSignerMatchedKid(
                    trustIssuer.kid(),
                    jwtStatementDomainService.generateEmptyTrustIssuerToken()
                )
            ) {
                log.error(
                    "Health Check failed for trustIssuer key configuration on kid {}. A generated signed Test-JWT could not be validated.",
                    trustIssuer.kid()
                );
                return KeyState.ERROR_KEY_MISMATCH;
            }
        } catch (DidResolverException | RestClientException e) {
            // Only log as warning as infrastructure currently is not that reliable,
            // and we will get OCC monitoring on that endpoint, so on consecutive failures we are still informed.
            log.warn(
                "Health Check failed for trustIssuer key configuration on kid {}. The DID could not be resolved.",
                trustIssuer.kid(),
                e
            );
            return KeyState.ERROR_RESOLVING;
        } catch (Exception e) {
            log.error(
                "Health Check failed for trustIssuer key configuration on kid {}. Private key might NOT match DID/kid.",
                trustIssuer.kid(),
                e
            );
            return KeyState.ERROR;
        }
        return KeyState.OK;
    }

    public enum KeyState {
        UNKNOWN,
        ERROR,
        ERROR_RESOLVING,
        ERROR_KEY_MISMATCH,
        OK,
    }

    @Scheduled(fixedRate = 300_000, initialDelay = 1000)
    void cronCheckTrustIssuerKeyValid() {
        this.trustIssuerKeyState = checkTrustIssuerKeyValid();
    }
}
