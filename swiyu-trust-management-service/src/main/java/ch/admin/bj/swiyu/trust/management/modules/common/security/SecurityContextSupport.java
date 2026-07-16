package ch.admin.bj.swiyu.trust.management.modules.common.security;

import ch.admin.bit.jeap.security.resource.token.JeapAuthenticationToken;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
@Slf4j
public class SecurityContextSupport {

    public static final String SYSTEM_USER = "SYSTEM";
    public static final String UNKNOWN_USER = "UNKNOWN";

    public static String getCurrentUserFullName() {
        var rawAuth = SecurityContextHolder.getContext().getAuthentication();
        if (rawAuth instanceof SystemUserAuthentication) {
            return SYSTEM_USER;
        } else if (rawAuth instanceof JeapAuthenticationToken auth) {
            return "%s %s".formatted(auth.getTokenGivenName(), auth.getTokenFamilyName());
        } else {
            return UNKNOWN_USER;
        }
    }

    public static String getCurrentUserName() {
        var rawAuth = SecurityContextHolder.getContext().getAuthentication();
        if (rawAuth instanceof SystemUserAuthentication) {
            return SYSTEM_USER;
        } else if (rawAuth instanceof JeapAuthenticationToken auth) {
            return "%s".formatted(auth.getPreferredUsername());
        } else {
            return UNKNOWN_USER;
        }
    }

    public static void setSystemUserAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(new SystemUserAuthentication());
    }
}
