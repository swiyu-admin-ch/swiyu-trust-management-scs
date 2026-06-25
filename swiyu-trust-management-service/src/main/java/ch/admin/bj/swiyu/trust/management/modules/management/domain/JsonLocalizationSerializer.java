package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.NonComplianceV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.ProtectedIssuanceAuthorizationV2Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.VerificationQueryV2Details;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

/**
 * Serializes the different language maps to a simple map of strings in the localization format
 * of Trust Protocol 2.0 / OpenID.
 *
 * @see <a href="https://confluence.bit.admin.ch/x/77DnUQ#TrustProtocol2.0-LocalizationLocalizationJWT">Trust Protocol 2.0</a>
 */
@UtilityClass
public class JsonLocalizationSerializer {

    private static final String LOCALIZATION_CONCATENATION = "%s#%s";

    public static Map<String, String> fromLocalizedIdentityV2DetailsLanguage(
        String node,
        Map<IdentityV2Details.Language, String> values
    ) {
        Map<String, String> map = new HashMap<>();
        if (!values.containsKey(IdentityV2Details.Language.DEFAULT)) {
            map.put(node, values.values().stream().findFirst().orElse(""));
        }
        for (var entry : values.entrySet()) {
            if (entry.getKey() == IdentityV2Details.Language.DEFAULT) {
                map.put(node, entry.getValue());
            } else {
                map.put(LOCALIZATION_CONCATENATION.formatted(node, entry.getKey().getLanguageCode()), entry.getValue());
            }
        }
        return map;
    }

    public static Map<String, String> fromLocalizedNonComplianceV2DetailsLanguage(
        String node,
        Map<NonComplianceV2Details.Language, String> values
    ) {
        Map<String, String> map = new HashMap<>();
        if (!values.containsKey(NonComplianceV2Details.Language.DEFAULT)) {
            map.put(node, values.values().stream().findFirst().orElse(""));
        }
        for (var entry : values.entrySet()) {
            if (entry.getKey() == NonComplianceV2Details.Language.DEFAULT) {
                map.put(node, entry.getValue());
            } else {
                map.put(LOCALIZATION_CONCATENATION.formatted(node, entry.getKey().getLanguageCode()), entry.getValue());
            }
        }
        return map;
    }

    public static Map<String, String> fromLocalizedProtectedIssuanceAuthorizationV2DetailsLanguage(
        String node,
        Map<ProtectedIssuanceAuthorizationV2Details.Language, String> values
    ) {
        Map<String, String> map = new HashMap<>();
        if (!values.containsKey(ProtectedIssuanceAuthorizationV2Details.Language.DEFAULT)) {
            map.put(node, values.values().stream().findFirst().orElse(""));
        }
        for (var entry : values.entrySet()) {
            if (entry.getKey() == ProtectedIssuanceAuthorizationV2Details.Language.DEFAULT) {
                map.put(node, entry.getValue());
            } else {
                map.put(LOCALIZATION_CONCATENATION.formatted(node, entry.getKey().getLanguageCode()), entry.getValue());
            }
        }
        return map;
    }

    public static Map<String, String> fromLocalizedVerificationQueryV2DetailsLanguage(
        String node,
        Map<VerificationQueryV2Details.Language, String> values
    ) {
        Map<String, String> map = new HashMap<>();
        if (!values.containsKey(VerificationQueryV2Details.Language.DEFAULT)) {
            map.put(node, values.values().stream().findFirst().orElse(""));
        }
        for (var entry : values.entrySet()) {
            if (entry.getKey() == VerificationQueryV2Details.Language.DEFAULT) {
                map.put(node, entry.getValue());
            } else {
                map.put(LOCALIZATION_CONCATENATION.formatted(node, entry.getKey().getLanguageCode()), entry.getValue());
            }
        }
        return map;
    }
}
