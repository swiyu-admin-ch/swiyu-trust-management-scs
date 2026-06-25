package ch.admin.bj.swiyu.trust.management.modules.common.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.trust.management.modules.common.i18n.ValidLocalizedMap;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.annotation.Annotation;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidLocalizedMapOpenApiExampleCustomizerTest {

    private ValidLocalizedMapOpenApiExampleCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new ValidLocalizedMapOpenApiExampleCustomizer();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void setsLocalizedExampleWhenValidLocalizedMapPresent() {
        var schema = new Schema();
        var type = mock(AnnotatedType.class);
        when(type.getCtxAnnotations()).thenReturn(new Annotation[] { validLocalizedMapAnnotation() });

        customizer.customize(schema, type);

        assertThat(schema.getExample()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        var example = (Map<String, String>) schema.getExample();
        assertThat(example.keySet()).first().isEqualTo("default");
        assertThat(example).containsKeys("default", "de-CH", "fr-CH", "it-CH", "en");
    }

    @Test
    @SuppressWarnings("rawtypes")
    void doesNotSetExampleWhenAnnotationAbsent() {
        var schema = new Schema();
        var type = mock(AnnotatedType.class);
        when(type.getCtxAnnotations()).thenReturn(new Annotation[0]);

        customizer.customize(schema, type);

        assertThat(schema.getExample()).isNull();
    }

    @Test
    @SuppressWarnings("rawtypes")
    void doesNotOverrideExistingExample() {
        var schema = new Schema();
        var existingExample = Map.of("default", "custom");
        schema.setExample(existingExample);
        var type = mock(AnnotatedType.class);
        when(type.getCtxAnnotations()).thenReturn(new Annotation[] { validLocalizedMapAnnotation() });

        customizer.customize(schema, type);

        assertThat(schema.getExample()).isSameAs(existingExample);
    }

    @Test
    @SuppressWarnings("rawtypes")
    void handlesNullCtxAnnotations() {
        var schema = new Schema();
        var type = mock(AnnotatedType.class);
        when(type.getCtxAnnotations()).thenReturn(null);

        customizer.customize(schema, type);

        assertThat(schema.getExample()).isNull();
    }

    private ValidLocalizedMap validLocalizedMapAnnotation() {
        return new ValidLocalizedMap() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return ValidLocalizedMap.class;
            }

            @Override
            public String message() {
                return "Map contains invalid locale key(s)";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }
        };
    }
}
