package ch.admin.bj.swiyu.trust.management.modules.common.auth;

import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import ch.admin.bit.jeap.security.resource.token.AuthoritiesResolver;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

/**
 * Converts the roles from the authentication token to known 'typed' Granted Authorities.
 * <p>Examples (token role -> UserRole:</p>
 * <ul>
 *     <li>[BJ-swiyu-tms.ALLOW,BJ-swiyu-tms.READER,BJ-swiyu-tms.EDITOR] -> [ROLE_READER, ROLE_EDITOR]</li>
 *     <li>[BJ-swiyu-tms.ALLOW,BJ-swiyu-tms.EDITOR] -> [ROLE_EDITOR]</li>
 *     <li>[BJ-swiyu-tms-int.ALLOW,BJ-swiyu-tms-int.EDITOR] -> [ROLE_EDITOR]</li>
 * </ul>
 *
 * <p>
 * see <a href="https://confluence.bit.admin.ch/display/BLUE/Autorisierung+gegen+Authorities">Autorisierung gegen Authorities</a>
 */
@Slf4j
public class JwtTokenAuthoritiesResolver implements AuthoritiesResolver {

    /**
     * Needed because on Preview Stages (e.g INT-ABN or INT-PROD) the audience is not BJ-SWIYU-TMS but
     * BJ-SWIYU-TMS-INT. The audience is used as role prefix, e.g. BJ-SWIYU-TMS.EDITOR.
     */
    private final String audience;

    public JwtTokenAuthoritiesResolver(String audience) {
        this.audience = requireNonNull(audience, "audience must be provided").toLowerCase();
    }

    @Override
    public Collection<GrantedAuthority> deriveAuthoritiesFromRoles(
        Set<String> tokenUserRoles,
        Map<String, Set<String>> businessPartnerRoles
    ) {
        if (isEmpty(tokenUserRoles)) {
            return emptySet();
        }
        return tokenUserRoles.stream().map(this::toUserRole).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private UserRole toUserRole(String tokenRoleNameWithAudiencePrefix) {
        if (!tokenRoleNameWithAudiencePrefix.toLowerCase().startsWith(audience + ".")) {
            log.info(
                "Token role {} does not match audience {}, ignoring it.",
                tokenRoleNameWithAudiencePrefix,
                audience
            );
            return null;
        }
        // get suffix, e.g. "EDITOR" from "BJ-SWIYU-TMS-INT.EDITOR"
        var roleName = tokenRoleNameWithAudiencePrefix.split("\\.", 2)[1];
        return UserRole.fromRoleName(roleName);
    }
}
