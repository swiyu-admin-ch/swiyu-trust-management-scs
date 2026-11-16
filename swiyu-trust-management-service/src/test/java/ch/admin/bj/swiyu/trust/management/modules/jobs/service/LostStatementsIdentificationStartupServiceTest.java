package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntity;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreEntityRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreStatus;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.Statement;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.StatementType;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LostStatementsIdentificationStartupServiceTest {

    @Mock
    private TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;

    @Mock
    private StatementRepository statementRepository;

    @Mock
    private DatastoreEntityRepository datastoreEntityRepository;

    @InjectMocks
    private LostStatementsIdentificationStartupService service;

    private static Statement statement(UUID id, boolean isActiveInStatuslist, boolean isSoftDeleted) {
        return new Statement(
            id,
            isSoftDeleted,
            isActiveInStatuslist,
            "serialized-jwt",
            null,
            StatementType.IDENTITY_TRUST_STATEMENT_V2
        );
    }

    private static DatastoreEntity datastoreEntity(DatastoreStatus status) {
        var entity = new DatastoreEntity();
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        entity.changeStatus(status);
        return entity;
    }

    private void givenRegistryStatements(Statement... statements) {
        when(statementRepository.findAllByIsActiveInStatuslistAndIsSoftDeleted(true, false)).thenReturn(
            Stream.of(statements)
        );
    }

    private void givenRegistryV1Statements(DatastoreEntity... entities) {
        when(datastoreEntityRepository.findAllByStatus(DatastoreStatus.ACTIVE)).thenReturn(Stream.of(entities));
    }

    private void givenStatementIsKnownToManagementDatabase(Statement statement, boolean exists) {
        when(
            trustStatementPartnerLinkRepository.existsByIdOrTrustRegistryEntryId(statement.getId(), statement.getId())
        ).thenReturn(exists);
    }

    private void givenStatementIsKnownToManagementDatabase(DatastoreEntity entity, boolean exists) {
        when(
            trustStatementPartnerLinkRepository.existsByIdOrTrustRegistryEntryId(entity.getId(), entity.getId())
        ).thenReturn(exists);
    }

    @Test
    void resolveV2Statements_marksLostStatementAsSoftDeleted() {
        // GIVEN
        var lostStatement = statement(UUID.randomUUID(), true, false);
        givenRegistryStatements(lostStatement);
        givenStatementIsKnownToManagementDatabase(lostStatement, false);

        // WHEN
        var modifiedCount = service.resolveV2Statements();

        // THEN
        assertThat(modifiedCount).isEqualTo(1);
        assertThat(lostStatement.isSoftDeleted()).isTrue();
        verify(statementRepository).saveAll(List.of(lostStatement));
    }

    @Test
    void resolveV2Statements_keepsStatementPresentInManagementDatabase() {
        // GIVEN
        var knownStatement = statement(UUID.randomUUID(), true, false);
        givenRegistryStatements(knownStatement);
        givenStatementIsKnownToManagementDatabase(knownStatement, true);

        // WHEN
        var modifiedCount = service.resolveV2Statements();

        // THEN
        assertThat(modifiedCount).isZero();
        assertThat(knownStatement.isSoftDeleted()).isFalse();
        verify(statementRepository).saveAll(List.of());
    }

    @Test
    void resolveV2Statements_onlyMarksLostStatementsAsSoftDeleted() {
        // GIVEN
        var lostStatement = statement(UUID.randomUUID(), true, false);
        var firstKnownStatement = statement(UUID.randomUUID(), true, false);
        var secondKnownStatement = statement(UUID.randomUUID(), true, false);
        givenRegistryStatements(lostStatement, firstKnownStatement, secondKnownStatement);
        givenStatementIsKnownToManagementDatabase(lostStatement, false);
        givenStatementIsKnownToManagementDatabase(firstKnownStatement, true);
        givenStatementIsKnownToManagementDatabase(secondKnownStatement, true);

        // WHEN
        var modifiedCount = service.resolveV2Statements();

        // THEN
        assertThat(modifiedCount).isEqualTo(1);
        assertThat(lostStatement.isSoftDeleted()).isTrue();
        assertThat(firstKnownStatement.isSoftDeleted()).isFalse();
        assertThat(secondKnownStatement.isSoftDeleted()).isFalse();
        verify(statementRepository).saveAll(List.of(lostStatement));
    }

    @Test
    void resolveV2Statements_withoutCandidatesModifiesNothing() {
        // GIVEN
        givenRegistryStatements();

        // WHEN
        var modifiedCount = service.resolveV2Statements();

        // THEN
        assertThat(modifiedCount).isZero();
        verify(statementRepository).saveAll(List.of());
    }

    @Test
    void resolveV1Statements_deactivatesLostDatastoreEntity() {
        // GIVEN
        var lostEntity = datastoreEntity(DatastoreStatus.ACTIVE);
        givenRegistryV1Statements(lostEntity);
        givenStatementIsKnownToManagementDatabase(lostEntity, false);

        // WHEN
        var modifiedCount = service.resolveV1Statements();

        // THEN
        assertThat(modifiedCount).isEqualTo(1);
        assertThat(lostEntity.getStatus()).isEqualTo(DatastoreStatus.DEACTIVATED);
        verify(datastoreEntityRepository).saveAll(List.of(lostEntity));
    }

    @Test
    void resolveV1Statements_keepsDatastoreEntityPresentInManagementDatabase() {
        // GIVEN
        var knownEntity = datastoreEntity(DatastoreStatus.ACTIVE);
        givenRegistryV1Statements(knownEntity);
        givenStatementIsKnownToManagementDatabase(knownEntity, true);

        // WHEN
        var modifiedCount = service.resolveV1Statements();

        // THEN
        assertThat(modifiedCount).isZero();
        assertThat(knownEntity.getStatus()).isEqualTo(DatastoreStatus.ACTIVE);
        verify(datastoreEntityRepository).saveAll(List.of());
    }

    @Test
    void resolveV1Statements_onlyDeactivatesLostDatastoreEntities() {
        // GIVEN
        var lostEntity = datastoreEntity(DatastoreStatus.ACTIVE);
        var firstKnownEntity = datastoreEntity(DatastoreStatus.ACTIVE);
        var secondKnownEntity = datastoreEntity(DatastoreStatus.ACTIVE);
        givenRegistryV1Statements(lostEntity, firstKnownEntity, secondKnownEntity);
        givenStatementIsKnownToManagementDatabase(lostEntity, false);
        givenStatementIsKnownToManagementDatabase(firstKnownEntity, true);
        givenStatementIsKnownToManagementDatabase(secondKnownEntity, true);

        // WHEN
        var modifiedCount = service.resolveV1Statements();

        // THEN
        assertThat(modifiedCount).isEqualTo(1);
        assertThat(lostEntity.getStatus()).isEqualTo(DatastoreStatus.DEACTIVATED);
        assertThat(firstKnownEntity.getStatus()).isEqualTo(DatastoreStatus.ACTIVE);
        assertThat(secondKnownEntity.getStatus()).isEqualTo(DatastoreStatus.ACTIVE);
        verify(datastoreEntityRepository).saveAll(List.of(lostEntity));
    }

    @Test
    void resolveV1Statements_withoutCandidatesModifiesNothing() {
        // GIVEN
        givenRegistryV1Statements();

        // WHEN
        var modifiedCount = service.resolveV1Statements();

        // THEN
        assertThat(modifiedCount).isZero();
        verify(datastoreEntityRepository).saveAll(List.of());
    }
}
