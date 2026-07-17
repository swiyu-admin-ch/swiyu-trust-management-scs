package ch.admin.bj.swiyu.trust.management.modules.common.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtTokenAuthoritiesResolverTest {

    @Test
    void deriveAuthoritiesFromRoles_withEditorAndAllow_returnsEditor() {
        // GIVEN
        var resolver = new JwtTokenAuthoritiesResolver("BJ-swiyu-TMS");
        var roles = Set.of("BJ-SWIYU-TMS.ALLOW", "BJ-SWIYU-TMS.EDITOR");
        // WHEN
        var result = resolver.deriveAuthoritiesFromRoles(roles, null);
        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(UserRole.EDITOR::equals));
    }

    @Test
    void deriveAuthoritiesFromRoles_withReaderEditorAndAllow_returnsReaderAndEditor() {
        // GIVEN
        var resolver = new JwtTokenAuthoritiesResolver("BJ-swiyu-TMS");
        var roles = Set.of("BJ-SWIYU-TMS.ALLOW", "BJ-SWIYU-TMS.READER", "BJ-SWIYU-TMS.EDITOR");
        // WHEN
        var result = resolver.deriveAuthoritiesFromRoles(roles, null);
        // THEN
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(UserRole.READER::equals));
        assertTrue(result.stream().anyMatch(UserRole.EDITOR::equals));
    }

    @Test
    void deriveAuthoritiesFromRoles_withEmptyRoles_returnsEmpty() {
        // GIVEN
        var resolver = new JwtTokenAuthoritiesResolver("BJ-swiyu-TMS");
        // WHEN
        var result = resolver.deriveAuthoritiesFromRoles(Collections.emptySet(), null);
        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deriveAuthoritiesFromRoles_intAudienceWithEditorAndAllow_returnsEditor() {
        // GIVEN – Preview/INT stage uses audience BJ-SWIYU-TMS-INT
        var resolver = new JwtTokenAuthoritiesResolver("BJ-swiyu-TMS-INT");
        var roles = Set.of("BJ-SWIYU-TMS-INT.ALLOW", "BJ-SWIYU-TMS-INT.EDITOR");
        // WHEN
        var result = resolver.deriveAuthoritiesFromRoles(roles, null);
        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.stream().anyMatch(UserRole.EDITOR::equals));
    }

    @Test
    void deriveAuthoritiesFromRoles_intAudienceRolesIgnoredForTmsAudience() {
        // GIVEN – resolver configured for TMS, but roles come from TMS-INT → no match
        var resolver = new JwtTokenAuthoritiesResolver("BJ-swiyu-TMS");
        var roles = Set.of("BJ-SWIYU-TMS-INT.ALLOW", "BJ-SWIYU-TMS-INT.EDITOR");
        // WHEN
        var result = resolver.deriveAuthoritiesFromRoles(roles, null);
        // THEN
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
