/**
 * Based on https://bitbucket.bit.admin.ch/projects/BIT_JME/repos/jme-security-oauth2-example/browse/jme-security-oauth2-claimsetconverter/src/main/java/ch/admin/bit/jeap/jme/security/oauth/resource/EiamClaimSetConverter.java
 */
package ch.admin.bj.swiyu.trust.management.modules.common.auth;

import static java.util.Collections.emptyMap;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.MappedJwtClaimSetConverter;
import org.springframework.stereotype.Component;

/**
 * Example claim set converter for converting certain claims of an access token issued by eAIM into the matching claims expected by jEAP.
 */
@Slf4j
@Component("eiamClaimSetConverter")
@RequiredArgsConstructor
public class EiamClaimSetConverter implements Converter<Map<String, Object>, Map<String, Object>> {

    // Converter for applying the spring security default mappings transforming some reserved claims
    private final Converter<Map<String, Object>, Map<String, Object>> defaultConverter =
        MappedJwtClaimSetConverter.withDefaults(emptyMap());

    @Override
    public Map<String, Object> convert(@NonNull Map<String, Object> claims) {
        Map<String, Object> mappedClaims = new HashMap<>(claims);
        mapClaim("role", "userroles", claims, mappedClaims);
        mapClaim("userExtId", "ext_id", claims, mappedClaims);
        mapClaim("email", "preferred_username", claims, mappedClaims);
        mapLocale(claims, mappedClaims);
        mappedClaims.put("ctx", "USER"); // assumption: just users and no systems in eIAM
        return defaultConverter.convert(mappedClaims);
    }

    private void mapClaim(
        String sourceClaimName,
        String targetClaimName,
        Map<String, Object> claims,
        Map<String, Object> mappedClaims
    ) {
        var sourceClaim = claims.get(sourceClaimName);
        if (sourceClaim != null) {
            mappedClaims.put(targetClaimName, sourceClaim);
        }
    }

    private void mapLocale(Map<String, Object> claims, Map<String, Object> mappedClaims) {
        var language = claims.get("language");
        if (language != null) {
            mappedClaims.put("locale", language.toString().toUpperCase());
        }
    }
}
