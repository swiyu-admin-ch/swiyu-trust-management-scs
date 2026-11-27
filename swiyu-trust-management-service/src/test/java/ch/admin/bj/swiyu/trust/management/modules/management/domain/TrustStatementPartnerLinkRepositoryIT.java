package ch.admin.bj.swiyu.trust.management.modules.management.domain;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink.createIdentityV1;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink.createIdentityV2;
import static ch.admin.bj.swiyu.trust.management.modules.management.domain.details.TrustStatementPartnerLinkType.STATEMENT_TOMBSTONE;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV1Details;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.details.IdentityV2Details;
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

    @Autowired
    private StatusListMetadataRepository statusListMetadataRepository;

    private UUID partnerA;
    private UUID partnerB;

    @BeforeEach
    void setUp() {
        repository.deleteAllInBatch();
        statusListMetadataRepository.deleteAllInBatch();
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

    @Test
    void existsByIdOrTrustRegistryEntryId_matchesEitherColumn() {
        // given
        var partnerLink = trustStatement(partnerA, "did:d");
        partnerLink.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        partnerLink = repository.saveAndFlush(partnerLink);
        var unknownId = UUID.randomUUID();

        // then
        assertThat(repository.existsByIdOrTrustRegistryEntryId(partnerLink.getId(), unknownId)).isTrue();
        assertThat(
            repository.existsByIdOrTrustRegistryEntryId(unknownId, partnerLink.getTrustRegistryEntryId())
        ).isTrue();
        assertThat(repository.existsByIdOrTrustRegistryEntryId(unknownId, unknownId)).isFalse();
    }

    @Test
    void findAllByPartnerIdOrSubjectIn_matchesByPartnerIdOrBySubject() {
        // when
        var result = repository.findAllByPartnerIdOrSubjectIn(partnerA, java.util.Set.of("did:c"));

        // then
        assertThat(result)
            .extracting(TrustStatementPartnerLink::getSubject)
            .containsExactlyInAnyOrder("did:a", "did:b", "did:c");
    }

    @Test
    void countByTypeNot_ignoresTombstones() {
        // given
        var tombstone = trustStatement(partnerA, "did:tombstone");
        tombstone.convertToTombstone();
        repository.saveAndFlush(tombstone);

        // then
        assertThat(repository.count()).isEqualTo(4);
        assertThat(repository.countByTypeNot(STATEMENT_TOMBSTONE)).isEqualTo(3);
    }

    @Test
    void findStatusListIndicesByStatusListMetadataIdAndStatus_selectsOnlyTheIndicesOfTheMatchingEntries() {
        // given one status list with a revoked and an active entry, one status list with a revoked entry only
        var statusListId = createStatusList();
        var otherStatusListId = createStatusList();
        repository.saveAndFlush(revokedStatement(statusListId, 3));
        repository.saveAndFlush(revokedStatement(statusListId, 7));
        repository.saveAndFlush(activeStatement(statusListId, 5));
        repository.saveAndFlush(revokedStatement(otherStatusListId, 9));

        // when
        var revokedIndices = repository.findStatusListIndicesByStatusListMetadataIdAndStatus(
            statusListId,
            TrustStatementPartnerLinkStatus.INACTIVE
        );

        // then only the revoked entries of that status list are returned
        assertThat(revokedIndices).containsExactlyInAnyOrder(3, 7);
        assertThat(
            repository.findStatusListIndicesByStatusListMetadataIdAndStatus(
                statusListId,
                TrustStatementPartnerLinkStatus.ACTIVE
            )
        ).containsExactly(5);
    }

    @Test
    void findStatusListIndicesByStatusListMetadataIdAndStatus_ignoresEntriesWithoutStatusListIndex() {
        // given a revoked statement that was never put on a status list
        var statusListId = createStatusList();
        repository.saveAndFlush(revokedStatement(statusListId, 1));
        var withoutIndex = trustStatement(partnerA, "did:no-status-list");
        withoutIndex.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.INACTIVE
        );
        repository.saveAndFlush(withoutIndex);

        // when
        var revokedIndices = repository.findStatusListIndicesByStatusListMetadataIdAndStatus(
            statusListId,
            TrustStatementPartnerLinkStatus.INACTIVE
        );

        // then
        assertThat(revokedIndices).containsExactly(1);
    }

    private UUID createStatusList() {
        var statusListId = UUID.randomUUID();
        statusListMetadataRepository.saveAndFlush(
            new StatusListMetadata(statusListId, "https://example.com/statuslist-" + statusListId + ".jwt", 100)
        );
        return statusListId;
    }

    private TrustStatementPartnerLink revokedStatement(UUID statusListId, int statusListIndex) {
        var statement = statementOnStatusList(statusListId, statusListIndex);
        statement.markAsInactive();
        return statement;
    }

    private TrustStatementPartnerLink activeStatement(UUID statusListId, int statusListIndex) {
        return statementOnStatusList(statusListId, statusListIndex);
    }

    private static TrustStatementPartnerLink statementOnStatusList(UUID statusListId, int statusListIndex) {
        var statement = createIdentityV2(
            UUID.randomUUID(),
            "did:status-list:" + statusListIndex,
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"),
            Map.of("en", "Name EN"),
            List.of(new IdentityV2Details.RegistryId("registryType1", "registryValue1")),
            false,
            new StatusListEntry(statusListId, statusListIndex)
        );
        // statements are created CONFIRMED and become active once they are published to the registry
        statement.persistReferencesAfterPublicationSucceeded(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TrustStatementPartnerLinkStatus.ACTIVE
        );
        return statement;
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
