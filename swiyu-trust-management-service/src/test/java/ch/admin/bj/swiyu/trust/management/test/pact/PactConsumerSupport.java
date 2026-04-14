package ch.admin.bj.swiyu.trust.management.test.pact;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationContext;
import ch.admin.bit.jeap.security.test.jws.JwsBuilderFactory;

/**
 * Helper class providing utility methods for Pact consumer tests.
 * Contains default methods for building JWS tokens, URL paths, and query parameters
 * commonly used in consumer-driven contract testing scenarios.
 */
public class PactConsumerSupport {

    // Use XXX (not X) for timezone offset to produce ISO-8601 compliant format (+01:00 instead of +01).
    // Java 25+ enforces strict parsing and rejects the short offset format.
    public static final String ISO_DATE_TIME_FORMAT_NANOSECONDS = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX";

    /**
     * Builds a JWS (JSON Web Signature) token for authentication in tests.
     * Creates a valid token with fixed long period for the specified subject and system context.
     *
     * @param jwsBuilderFactory the factory used to create JWS token builders
     * @param subject the subject identifier for whom the token is issued
     * @param roles the roles to be assigned to the user in the token
     * @return a serialized JWS token string that can be used in authorization headers
     * @throws IllegalArgumentException if jwsBuilderFactory or subject is null
     */
    public static String buildJwsToken(JwsBuilderFactory jwsBuilderFactory, String subject, String... roles) {
        return jwsBuilderFactory
            .createValidForFixedLongPeriodBuilder(subject, JeapAuthenticationContext.SYS)
            .withUserRoles(roles)
            .build()
            .serialize();
    }
}
