package ch.admin.bj.swiyu.trust.management.modules.common.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class LocalizedMapUtilTest {

    @Test
    void getDefaultValue_returnsDefaultEntry() {
        assertThat(LocalizedMapUtil.getDefaultValue(Map.of("default", "Acme", "de-CH", "Acme DE"))).isEqualTo("Acme");
    }

    @Test
    void getDefaultValue_nullMap_returnsNull() {
        assertThat(LocalizedMapUtil.getDefaultValue(null)).isNull();
    }

    @Test
    void fromSingleName_producesOnlyDefaultEntry() {
        assertThat(LocalizedMapUtil.fromSingleName("Acme")).containsExactlyEntriesOf(Map.of("default", "Acme"));
    }

    @Test
    void fromLanguages_skipsBlankTranslationsAndKeepsDefault() {
        var result = LocalizedMapUtil.fromLanguages("Acme", "Acme DE", "  ", null, "Acme EN", "");

        assertThat(result)
            .containsEntry("default", "Acme")
            .containsEntry("de-CH", "Acme DE")
            .containsEntry("en", "Acme EN")
            .doesNotContainKeys("fr-CH", "it-CH", "rm-CH");
    }
}
