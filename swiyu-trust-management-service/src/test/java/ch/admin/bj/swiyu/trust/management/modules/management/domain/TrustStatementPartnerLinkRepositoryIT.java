package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink.createIdentityV1;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = PostgreSQLContainerInitializer.class)
@DataJpaTest
@Import(DataJpaTestConfiguration.class)
@ActiveProfiles("test")
class TrustStatementPartnerLinkRepositoryIT {

    @Autowired
    private TrustStatementPartnerLinkRepository repository;

    private UUID partnerA;
    private UUID partnerB;

    @BeforeEach
    void setUp() {
        repository.deleteAllInBatch();
        partnerA = UUID.randomUUID();
        partnerB = UUID.randomUUID();
        repository.saveAndFlush(trustStatement(partnerA, "did:a"));
        repository.saveAndFlush(trustStatement(partnerA, "did:b"));
        repository.saveAndFlush(trustStatement(partnerB, "did:c"));
    }

    @Test
    void deleteAllByPartnerId_deletesOnlyThatPartnersStatements() {
        // when
        repository.deleteAllByPartnerId(partnerA);

        // then
        assertThat(repository.findAll()).extracting(TrustStatementPartnerLink::getPartnerId).containsOnly(partnerB);
    }

    private static TrustStatementPartnerLink trustStatement(UUID partnerId, String subject) {
        return createIdentityV1(
            partnerId,
            subject,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Map.of("de-CH", "Name DE", "en", "Name EN", "fr-CH", "Nom FR", "it-CH", "Nome IT", "rm-CH", "Numn RM"),
            List.of(new IdentityV1Details.RegistryId("registryType1", "registryValue1")),
            true
        );
    }
}
