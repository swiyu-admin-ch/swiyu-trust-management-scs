package ch.admin.bj.swiyu.trust.management.modules.common.security;

import java.util.*;
import lombok.extern.slf4j.*;
import org.springframework.security.authentication.*;

@Slf4j
public class SystemUserAuthentication extends AbstractAuthenticationToken {

    public static String SYSTEM_USER = "SYSTEM";

    public SystemUserAuthentication() {
        super(new ArrayList<>() /* here we could later even define the permissions */);
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return SYSTEM_USER;
    }
}
