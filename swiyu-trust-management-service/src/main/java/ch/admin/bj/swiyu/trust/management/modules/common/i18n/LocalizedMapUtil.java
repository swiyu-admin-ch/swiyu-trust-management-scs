package ch.admin.bj.swiyu.trust.management.modules.common.i18n;

import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DEFAULT_VALUE_KEY;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.DE_CH;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.EN_CH;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.FR_CH;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.IT_CH;
import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.LocalizedMapConstants.RM_CH;

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
        putIfPresent(map, DE_CH, de);
        putIfPresent(map, FR_CH, fr);
        putIfPresent(map, IT_CH, it);
        putIfPresent(map, EN_CH, en);
        putIfPresent(map, RM_CH, rm);
        return Map.copyOf(map);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }
}
