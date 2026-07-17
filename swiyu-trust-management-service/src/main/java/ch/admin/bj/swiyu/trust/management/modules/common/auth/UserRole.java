package ch.admin.bj.swiyu.trust.management.modules.common.auth;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.GrantedAuthority;

/**
 * All supported UserRoles as GrantedAuthority that are used to check authorization against.
 *
 */
public enum UserRole implements GrantedAuthority {
    EDITOR(Names.EDITOR),
    READER(Names.READER);

    /**
     * The name as it is used in the application.
     */
    @Getter
    private final String roleName;

    UserRole(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public String getAuthority() {
        return "ROLE_" + this.getRoleName(); // role authorities have in spring the default prefix "ROLE_"
    }

    public static UserRole fromRoleName(String roleName) {
        for (UserRole role : values()) {
            if (role.getRoleName().equalsIgnoreCase(roleName)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Contains the names of the supported UserRoles.Public so we can use it also in tests.
     */
    @UtilityClass
    public static class Names {

        public static final String READER = "READER";
        public static final String EDITOR = "EDITOR";
    }

    @UtilityClass
    public static class Expressions {

        public static final String HAS_ROLE_EDITOR_OR_READER =
            "hasAnyRole('" + Names.EDITOR + "', '" + Names.READER + "')";
        public static final String HAS_ROLE_EDITOR = "hasRole('" + Names.EDITOR + "')";
    }
}
