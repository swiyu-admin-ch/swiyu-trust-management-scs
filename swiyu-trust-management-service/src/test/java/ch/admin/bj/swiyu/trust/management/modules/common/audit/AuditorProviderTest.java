package ch.admin.bj.swiyu.trust.management.modules.common.audit;

import static ch.admin.bj.swiyu.trust.management.modules.common.audit.AuditorProvider.getCurrentAuditor;
import static java.util.Collections.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.*;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import ch.admin.bj.swiyu.trust.management.modules.common.security.SystemUserAuthentication;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuditorProviderTest {

    @Test
    void getCurrentAuditor_eiamUser() {
        // GIVEN
        var auth = new JeapAuthenticationToken(jwtEiam(), emptySet(), emptyMap(), emptyList());
        auth.setAuthenticated(true);
        // WHEN
        var auditor = getCurrentAuditor(auth);
        // THEN
        assertFalse(auditor.isAnonymous());
        assertFalse(auditor.isSystem());
        assertEquals("47443696:Mustermann,Max", auditor.auditUserId());
        assertEquals("https://identity-eiam-r.eiam.admin.ch/realms/ejpd_bj-swiyu-tms", auditor.identityProvider());
    }

    @Test
    void getCurrentAuditor_anonymous() {
        // GIVEN
        var auth = new JeapAuthenticationToken(jwtAnonymous(), emptySet(), emptyMap(), emptyList());
        auth.setAuthenticated(true);
        // WHEN
        var auditor = getCurrentAuditor(auth);
        // THEN
        assertTrue(auditor.isAnonymous());
        assertEquals("ANONYMOUS", auditor.auditUserId());
    }

    @Test
    void getCurrentAuditor_system() {
        // GIVEN
        var auth = new SystemUserAuthentication();
        // WHEN
        var auditor = getCurrentAuditor(auth);
        // THEN
        assertTrue(auditor.isSystem());
        assertEquals("SYSTEM", auditor.auditUserId());
    }

    @Test
    void getCurrentAuditor_unauthenticated() {
        // GIVEN
        var auth = new JeapAuthenticationToken(jwtAnonymous(), emptySet(), emptyMap(), emptyList());
        auth.setAuthenticated(false);
        // WHEN / THEN
        assertThrows(AuditorProvider.UserNotAuthenticatedException.class, () -> getCurrentAuditor(auth));
    }

    @Test
    void getCurrentAuditor_unknownAuthentication() {
        // GIVEN
        var auth = new JwtAuthenticationToken(jwt(Map.of("randomClain", "--")), emptySet());
        auth.setAuthenticated(true);
        // WHEN / THEN
        assertThrows(AuditorProvider.UnknownAuthenticationException.class, () -> getCurrentAuditor(auth));
    }

    private static Jwt jwtEiam() {
        return jwt(
            Map.of(
                "iss",
                "https://identity-eiam-r.eiam.admin.ch/realms/ejpd_bj-swiyu-tms",
                "sub",
                "47443696",
                "given_name",
                "Max",
                "family_name",
                "Mustermann"
            )
        );
    }

    private static Jwt jwtAnonymous() {
        return jwt(Map.of("locale", "de"));
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt("token", Instant.now(), Instant.now().plusSeconds(100), Map.of("alg", "RS256"), claims);
    }
}
