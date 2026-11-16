package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import static ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkStatus.ACTIVE;
import static ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreStatus.DEACTIVATED;
import static org.assertj.core.api.Assertions.assertThat;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLink;
import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntity;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntityRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreStatus;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.Statement;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementType;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcEntityRepository;
import ch.admin.bj.swiyu.trust.management.test.DataJpaTestConfiguration;
import ch.admin.bj.swiyu.trust.management.test.PostgreSQLContainerInitializer;
import ch.admin.bj.swiyu.trust.management.test.TrustStatementTestData;
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
@Import({ LostStatementsIdentificationStartupService.class, DataJpaTestConfiguration.class })
@ActiveProfiles("test")
class LostStatementsIdentificationStartupServiceIT {

    @Autowired
    private LostStatementsIdentificationStartupService lostStatementsIdentificationStartupService;

    @Autowired
    private StatementRepository statementRepository;

    @Autowired
    private DatastoreEntityRepository datastoreEntityRepository;

    @Autowired
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Autowired
    private VcEntityRepository vcEntityRepository;

    @BeforeEach
    void setUp() {
        statementRepository.deleteAllInBatch();
        // vc_entity references datastore_entity, therefore it has to be cleaned up first
        vcEntityRepository.deleteAllInBatch();
        datastoreEntityRepository.deleteAllInBatch();
        trustStatementPartnerLinkRepository.deleteAllInBatch();
    }

    private Statement persistStatement(boolean isActiveInStatuslist, boolean isSoftDeleted) {
        return statementRepository.save(
            new Statement(
                UUID.randomUUID(),
                isSoftDeleted,
                isActiveInStatuslist,
                "serialized-jwt",
                null,
                StatementType.IDENTITY_TRUST_STATEMENT_V2
            )
        );
    }

    private DatastoreEntity persistDatastoreEntity(DatastoreStatus status) {
        var datastoreEntity = new DatastoreEntity();
        datastoreEntity.changeStatus(status);
        return datastoreEntityRepository.save(datastoreEntity);
    }

    private TrustStatementPartnerLink persistPartnerLinkReferencingTrustRegistryEntry(UUID trustRegistryEntryId) {
        var partnerLink = TrustStatementTestData.issuanceV1("did:example:issuer");
        partnerLink.persistReferencesAfterPublicationSucceeded(UUID.randomUUID(), trustRegistryEntryId, ACTIVE);
        return trustStatementPartnerLinkRepository.save(partnerLink);
    }

    private boolean isSoftDeleted(UUID statementId) {
        return statementRepository.findById(statementId).orElseThrow().isSoftDeleted();
    }

    private DatastoreStatus statusOf(UUID datastoreEntityId) {
        return datastoreEntityRepository.findById(datastoreEntityId).orElseThrow().getStatus();
    }

    @Test
    void resolveV2Statements_softDeletesStatementMissingInManagementDatabase() {
        // GIVEN
        var lostStatement = persistStatement(true, false);
        var knownStatement = persistStatement(true, false);
        persistPartnerLinkReferencingTrustRegistryEntry(knownStatement.getId());

        // WHEN
        var modifiedCount = lostStatementsIdentificationStartupService.resolveV2Statements();

        // THEN
        assertThat(modifiedCount).isEqualTo(1);
        assertThat(isSoftDeleted(lostStatement.getId())).isTrue();
        assertThat(isSoftDeleted(knownStatement.getId())).isFalse();
    }

    @Test
    void resolveV2Statements_ignoresStatementsInactiveInStatuslistOrAlreadySoftDeleted() {
        // GIVEN
        var inactiveStatement = persistStatement(false, false);
        var softDeletedStatement = persistStatement(true, true);

        // WHEN
        var modifiedCount = lostStatementsIdentificationStartupService.resolveV2Statements();

        // THEN
        assertThat(modifiedCount).isZero();
        assertThat(isSoftDeleted(inactiveStatement.getId())).isFalse();
        assertThat(isSoftDeleted(softDeletedStatement.getId())).isTrue();
    }

    @Test
    void resolveV1Statements_deactivatesDatastoreEntityMissingInManagementDatabase() {
        // GIVEN
        var lostDatastoreEntity = persistDatastoreEntity(DatastoreStatus.ACTIVE);
        var knownDatastoreEntity = persistDatastoreEntity(DatastoreStatus.ACTIVE);
        persistPartnerLinkReferencingTrustRegistryEntry(knownDatastoreEntity.getId());

        // WHEN
        var modifiedCount = lostStatementsIdentificationStartupService.resolveV1Statements();

        // THEN
        assertThat(modifiedCount).isEqualTo(1);
        assertThat(statusOf(lostDatastoreEntity.getId())).isEqualTo(DEACTIVATED);
        assertThat(statusOf(knownDatastoreEntity.getId())).isEqualTo(DatastoreStatus.ACTIVE);
    }

    @Test
    void resolveV1Statements_ignoresDatastoreEntitiesNotActive() {
        // GIVEN
        var deactivatedDatastoreEntity = persistDatastoreEntity(DEACTIVATED);

        // WHEN
        var modifiedCount = lostStatementsIdentificationStartupService.resolveV1Statements();

        // THEN
        assertThat(modifiedCount).isZero();
        assertThat(statusOf(deactivatedDatastoreEntity.getId())).isEqualTo(DEACTIVATED);
    }
}
