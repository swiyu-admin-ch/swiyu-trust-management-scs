package ch.admin.bj.swiyu.trust.management.modules.management.domain.details;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * This tests is to make sure that the JSON serialization of all trust statement data types is configured correctly.
 */
class TrustStatementPartnerLinkDetailsTest {

    @Test
    void verifyAllTypesAreConfiguredForJsonSerialization() {
        var clazz = TrustStatementDetails.class;

        // Check if @JsonSubTypes annotation is present on the class
        assertTrue(clazz.isAnnotationPresent(JsonSubTypes.class), "@JsonSubTypes annotation is missing.");

        // Get all @JsonSubType annotations
        JsonSubTypes jsonSubTypesAnnotation = clazz.getAnnotation(JsonSubTypes.class);
        JsonSubTypes.Type[] jsonSubTypeAnnotations = jsonSubTypesAnnotation.value();

        var configuredSubTypeNames = Arrays.stream(jsonSubTypeAnnotations).map(JsonSubTypes.Type::name).toList();

        // 1. check all enum values are within configuredSubTypeNames
        for (TrustStatementType type : TrustStatementType.values()) {
            assertTrue(
                configuredSubTypeNames.contains(type.name()),
                "Missing Annotation @JsonSubTypes.Type for type: " +
                    type.name() +
                    " on the class " +
                    clazz.getSimpleName() +
                    ". Please add the entry."
            );
        }
        // 2. check all configuredSubTypeNames are also in TrustStatementType
        for (String configuredSubTypeName : configuredSubTypeNames) {
            assertTrue(
                Arrays.stream(TrustStatementType.values()).anyMatch(type -> type.name().equals(configuredSubTypeName)),
                "Missing trust statement type value " +
                    configuredSubTypeName +
                    ". It is mentioned in the class but is not present in the enum."
            );
        }
    }
}
