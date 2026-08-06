package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;

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

    /**
     * Example:
     * toLocalizedClaims("entity_name", {"default": "Migros", "de-CH": "Migros", "fr-CH": "Migros(fr)"})
     * returns {"entity_name": "Migros", "entity_name#de-CH": "Migros", "entity_name#fr-CH": "Migros(fr)"}.
     */
    public static Map<String, String> toLocalizedClaims(String node, Map<String, String> values) {
        Map<String, String> map = new HashMap<>();
        if (!values.containsKey(DEFAULT_VALUE_KEY)) {
            map.put(node, values.values().stream().findFirst().orElse(""));
        }
        for (var entry : values.entrySet()) {
            if (DEFAULT_VALUE_KEY.equals(entry.getKey())) {
                map.put(node, entry.getValue());
            } else {
                map.put(LOCALIZATION_CONCATENATION.formatted(node, entry.getKey()), entry.getValue());
            }
        }
        return map;
    }
}
