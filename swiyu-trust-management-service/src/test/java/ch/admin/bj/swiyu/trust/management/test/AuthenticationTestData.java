package ch.admin.bj.swiyu.trust.management.test;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

public class AuthenticationTestData {

    public static Authentication authentication() {
        return new TestingAuthenticationToken("test-user", "N/A");
    }
}
