package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.MANAGEMENT_TRANSACTION_MANAGER;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.MetadataV1Details;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import({ DataJpaTestConfiguration.class })
@ActiveProfiles("test")
// Will be removed with EID-5295
class TSMetadataV1MigrationIT {

    @Autowired
    TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    TSMetadataV1Repository tsMetadataV1Repository; // NOSONAR

    @Value("classpath:db/migration/management/V2_0_2__trust_statement_data_migrate.sql")
    Resource migrateScript;

    @Autowired
    TransactionTemplate transaction;

    @Autowired
    EntityManager entityManager;

    @Autowired
    @Qualifier(MANAGEMENT_TRANSACTION_MANAGER)
    PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        transaction.setTransactionManager(transactionManager);
    }

    @Test
    void testFlywayMigration() {
        // GIVEN (a non-migrated TSMetadataV1)
        var statementId = transaction.execute(transactionStatus ->
            tsMetadataV1Repository.saveAndFlush(tsMetadataV1()).getId()
        );

        assertThat(trustStatementPartnerLinkRepository.existsById(statementId)).isTrue();

        // WHEN (migration script is executed)
        transaction.executeWithoutResult(transactionStatus -> {
            try {
                var sql = new String(migrateScript.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                entityManager.createNativeQuery(sql).executeUpdate();
            } catch (Exception e) {
                throw new AssertionError("Failed to execute migration script", e);
            }
        });
        entityManager.flush();
        entityManager.clear();

        // THEN (the TSMetadataV1 is migrated to TrustStatementPartnerLink with MetadataV1Details)
        transaction.executeWithoutResult(transactionStatus -> {
            var statement = trustStatementPartnerLinkRepository.findById(statementId).orElseThrow();
            if (statement.getDetails() instanceof MetadataV1Details details) {
                assertThat(details).isNotNull();
                assertThat(statement.getAudit().getLastModifiedBy()).isEqualTo("migration_script");
                assertThat(details.getPreferredLanguage()).isEqualTo(MetadataV1Details.Language.DE_CH);
                assertThat(details.getOrgName()).containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        MetadataV1Details.Language.EN,
                        "Organization EN",
                        MetadataV1Details.Language.DE_CH,
                        "Organization DE_CH",
                        MetadataV1Details.Language.FR_CH,
                        "Organization FR_CH",
                        MetadataV1Details.Language.RM_CH,
                        "Organization RM_CH"
                    )
                );
                assertThat(details.getLogoUri()).containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        MetadataV1Details.Language.EN,
                        "https://example.com/logo-en.png",
                        MetadataV1Details.Language.DE_CH,
                        "https://example.com/logo-de.png",
                        MetadataV1Details.Language.FR_CH,
                        "https://example.com/logo-fr.png",
                        MetadataV1Details.Language.RM_CH,
                        "https://example.com/logo-rm.png"
                    )
                );
            } else {
                throw new AssertionError("Expected details to be of type MetadataV1Details");
            }
        });
    }

    private static TSMetadataV1 tsMetadataV1() {
        // NOSONAR
        return new TSMetadataV1(
            /* NOSONAR */ "testSubject", // subject
            Instant.parse("2023-01-01T00:00:00Z"), // validFrom
            Instant.parse("2023-12-31T23:59:59Z"), // validUntil
            "de-CH", // preferredLanguage
            "Organization EN", // orgNameEn
            "Organization DE_CH", // orgNameDeCh
            "Organization FR_CH", // orgNameFrCh
            null, // orgNameItCh
            "Organization RM_CH", // orgNameRmCh
            "https://example.com/logo-en.png", // logoUriEn
            "https://example.com/logo-de.png", // logoUriDeCh
            "https://example.com/logo-fr.png", // logoUriFrCh
            null, // logoUriItCh
            "https://example.com/logo-rm.png" // logoUriRmCh
        );
    }
}
