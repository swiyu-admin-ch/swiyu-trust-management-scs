package ch.admin.bj.swiyu.trust.management.modules.common.i18n;

import static ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMapValidator.validateLocalizedMap;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.validation.ValidationException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidLocalizedMapValidatorTest {

    @Test
    void validateLocalizedMap_whenMapIsEmpty_throwValidationException() {
        var map = new HashMap<String, String>();
        assertThatThrownBy(() -> validateLocalizedMap(map))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Map cannot be null or empty");
    }

    @Test
    void validateLocalizedMap_whenMapIsNull_throwValidationException() {
        assertThatThrownBy(() -> validateLocalizedMap(null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Map cannot be null or empty");
    }

    @Test
    void validateLocalizedMap_whenDefaultKeyMissing_throwValidationException() {
        var map = Map.of("de-CH", "Deutsch", "en", "English");
        assertThatThrownBy(() -> validateLocalizedMap(map))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("default");
    }

    @Test
    void validateLocalizedMap_whenAllKeysAreValidLocalesWithDefault_thenNoExceptionThrown() {
        var map = Map.of("default", "Default value", "de-CH", "Deutsch", "fr-CH", "Français", "it-CH", "Italiano");
        assertThatNoException().isThrownBy(() -> validateLocalizedMap(map));
    }

    @Test
    void validateLocalizedMap_whenKeyIsInvalidLocale_throwValidationException() {
        var map = Map.of("default", "Default value", "cheese", "value");
        assertThatThrownBy(() -> validateLocalizedMap(map)).isInstanceOf(ValidationException.class);
    }

    @Test
    void validateLocalizedMap_whenOneKeyIsInvalidAmongValidOnes_throwValidationException() {
        var map = Map.of("default", "Default value", "de-CH", "Deutsch", "cheese", "value");
        assertThatThrownBy(() -> validateLocalizedMap(map)).isInstanceOf(ValidationException.class);
    }
}
