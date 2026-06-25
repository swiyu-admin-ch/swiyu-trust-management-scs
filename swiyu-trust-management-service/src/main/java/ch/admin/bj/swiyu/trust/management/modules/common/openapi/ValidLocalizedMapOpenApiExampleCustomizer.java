package ch.admin.bj.swiyu.trust.management.modules.common.openapi;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.stereotype.Component;

/**
 * Customize the example values of @ValidLocalizedMap properties in the OpenApi doc
 */
@Component
public class ValidLocalizedMapOpenApiExampleCustomizer implements PropertyCustomizer {

    private static final LinkedHashMap<String, String> LOCALIZED_MAP_EXAMPLE = new LinkedHashMap<>();

    static {
        LOCALIZED_MAP_EXAMPLE.put("default", "value: default");
        LOCALIZED_MAP_EXAMPLE.put("en", "value: english");
        LOCALIZED_MAP_EXAMPLE.put("de-CH", "value: german");
        LOCALIZED_MAP_EXAMPLE.put("fr-CH", "value: french");
        LOCALIZED_MAP_EXAMPLE.put("it-CH", "value: italian");
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Schema customize(Schema schema, AnnotatedType type) {
        boolean hasValidLocalizedMap = Arrays.stream(
            Optional.ofNullable(type.getCtxAnnotations()).orElse(new Annotation[0])
        ).anyMatch(ValidLocalizedMap.class::isInstance);

        if (hasValidLocalizedMap && schema.getExample() == null) {
            schema.setExample(new LinkedHashMap<>(LOCALIZED_MAP_EXAMPLE));
        }
        return schema;
    }
}
