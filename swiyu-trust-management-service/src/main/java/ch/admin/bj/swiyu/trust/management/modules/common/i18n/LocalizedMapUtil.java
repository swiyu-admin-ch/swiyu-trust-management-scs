package ch.admin.bj.swiyu.trust.management.modules.common.i18n;

import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LocalizedMapUtil {

    public static String getDefaultValue(Map<String, String> map) {
        return map == null ? null : map.get(DEFAULT_VALUE_KEY);
    }

    public static Map<String, String> fromSingleName(String name) {
        return Map.of(DEFAULT_VALUE_KEY, name);
    }

    public static Map<String, String> fromLanguages(
        String defaultValue,
        String de,
        String fr,
        String it,
        String en,
        String rm
    ) {
        var map = new LinkedHashMap<String, String>();
        map.put(DEFAULT_VALUE_KEY, defaultValue);
        putIfPresent(map, "de-CH", de);
        putIfPresent(map, "fr-CH", fr);
        putIfPresent(map, "it-CH", it);
        putIfPresent(map, "en", en);
        putIfPresent(map, "rm-CH", rm);
        return Map.copyOf(map);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
