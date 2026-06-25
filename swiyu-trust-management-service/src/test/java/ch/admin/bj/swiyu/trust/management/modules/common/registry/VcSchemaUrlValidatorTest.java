package ch.admin.bj.swiyu.trust.management.modules.common.registry;

import static org.junit.Assert.*;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaUrlValidationFailedException;
import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class VcSchemaUrlValidatorTest {

    RegistryProperties registryProperties = new RegistryProperties(
        URI.create("http://dummy-test-trust-registry.ch/"),
        "/api/v1/vc-schema"
    );
    VcSchemaUrlValidator vcSchemaUrlValidator = new VcSchemaUrlValidator(registryProperties);

    @ParameterizedTest
    @ValueSource(
        strings = {
            "my/Path",
            "../myPath",
            "/my:::Path",
            "/../myPath",
            "/my?Path",
            "/my#Path",
            "",
            "/myPath/",
            "/myPath_",
            "/myPath-",
            "/myPath?asd=asd",
            "/",
        }
    )
    void createVcSchemaWithInvalidVctPath(String userProvidedLastPathSegment) {
        // GIVEN vc schema file with invalid userProvidedLastPathSegment
        var vct = String.format("http://dummy-test-trust-registry.ch/api/v1/vc-schema%s", userProvidedLastPathSegment);
        var e = assertThrows(VcSchemaUrlValidationFailedException.class, () ->
            vcSchemaUrlValidator.getVctResourcePath(vct)
        );
        // THEN exception is thrown
        assertTrue(
            e.getMessage().startsWith("Vct does not match expected pattern for user provided last path segment")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = { "/myPath", "/myPath12", "/myPa_t-h", "/myPa_t/a-h" })
    void createVcSchemaWithValidVctPath(String userProvidedLastPathSegment) {
        // GIVEN vc schema file with valid userProvidedLastPathSegment
        var path = vcSchemaUrlValidator.getVctResourcePath(
            String.format("http://dummy-test-trust-registry.ch/api/v1/vc-schema%s", userProvidedLastPathSegment)
        );
        var storedPath = "/%s".formatted(path);
        assertEquals(storedPath, userProvidedLastPathSegment);
    }
}
