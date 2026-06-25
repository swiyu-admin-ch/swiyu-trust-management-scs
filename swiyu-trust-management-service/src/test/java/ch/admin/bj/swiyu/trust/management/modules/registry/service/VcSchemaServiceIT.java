package ch.admin.bj.swiyu.trust.management.modules.registry.service;

import static org.junit.Assert.*;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaPublicationFailedException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaUrlValidationFailedException;
import ch.admin.bj.swiyu.trust.management.modules.common.registry.VcSchemaUrlValidator;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcSchemaRepository;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ VcSchemaService.class, DataJpaTestConfiguration.class })
@ActiveProfiles("test")
public class VcSchemaServiceIT {

    @Autowired
    VcSchemaRepository vcSchemaRepository;

    @Autowired
    VcSchemaService vcSchemaService;

    @Autowired
    VcSchemaUrlValidator vcSchemaUrlValidator;

    @BeforeEach
    void setUp() {
        vcSchemaRepository.deleteAllInBatch();
    }

    @Test
    void createVcSchemaWithInvalidFile() {
        // GIVEN non serializable vc schema file
        var e = assertThrows(VcSchemaPublicationFailedException.class, () ->
            vcSchemaService.publishVcSchema("invalidFile")
        );
        // THEN exception is thrown
        assertTrue(e.getMessage().startsWith("File of vc schema submission is invalid json"));
    }

    @Test
    void createVcSchemaWithMissingVctProperty() {
        // GIVEN vc schema file with invalid domain
        var e = assertThrows(VcSchemaPublicationFailedException.class, () ->
            vcSchemaService.publishVcSchema(
                """
                {
                "attr1": "https://bit.admin.ch"
                }
                """
            )
        );
        // THEN exception is thrown
        assertTrue(e.getMessage().startsWith("File of vc schema submission doesn't contain vct property"));
    }

    @Test
    void createVcSchemaWithInvalidVctPath() {
        // GIVEN vc schema file with invalid userProvidedLastPathSegment
        var e = assertThrows(VcSchemaUrlValidationFailedException.class, () ->
            vcSchemaService.publishVcSchema(
                """
                {
                "vct": "http://dummy-test-trust-registry.ch/api/v1/vc-schema/my#Path"
                }
                """
            )
        );
        // THEN exception is thrown
        assertTrue(
            e.getMessage().startsWith("Vct does not match expected pattern for user provided last path segment")
        );
    }

    @Test
    void createVcSchemaWithValidVctPath() {
        // GIVEN vc schema file with valid userProvidedLastPathSegment
        var validPath = "/myPath12";
        var dto = vcSchemaService.publishVcSchema(
            String.format(
                """
                {
                "vct": "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema%s"
                }
                """,
                validPath
            )
        );
        var storedPath = "/%s".formatted(dto.path());
        assertEquals(storedPath, validPath);
    }

    @Test
    void createVcSchemaWithValidFile() {
        // GIVEN stored vc schema with valid file
        var rawSchema = """
            {
            "vct": "https://trust-reg.trust-infra.swiyu.admin.ch/api/v1/vc-schema/mySchema1",
            "attr1": "value1"
            }
            """;
        var publishDto = vcSchemaService.publishVcSchema(rawSchema);

        // WHEN retrieve stored vc schema
        var dbVcSchema = vcSchemaRepository.findByPath(publishDto.path());

        // THEN File content matched user provided file
        assertTrue(dbVcSchema.isPresent());
        var dto = dbVcSchema.get();
        assertEquals(dto.getFile(), rawSchema);
    }
}
