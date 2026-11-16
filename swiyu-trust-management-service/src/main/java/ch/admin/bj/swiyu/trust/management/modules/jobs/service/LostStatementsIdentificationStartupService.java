package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.TrustStatementPartnerLinkRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.*;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LostStatementsIdentificationStartupService {

    private final TrustStatementPartnerLinkRepository trustStatementPartnerLinkRepository;
    private final StatementRepository statementRepository;
    private final DatastoreEntityRepository datastoreEntityRepository;

    @Transactional
    public int resolveV2Statements() {
        var registryStatements = statementRepository.findAllByIsActiveInStatuslistAndIsSoftDeleted(true, false);
        var modifiedRegistryStatements = new ArrayList<Statement>();
        registryStatements.forEach(registryStatement -> {
            if (
                !trustStatementPartnerLinkRepository.existsByIdOrTrustRegistryEntryId(
                    registryStatement.getId(),
                    registryStatement.getId()
                )
            ) {
                log.warn(
                    "LostStatementsIdentification: Statement {} exists in registry DB but not management DB. Type: {}. Marking as soft deleted.",
                    registryStatement.getId(),
                    registryStatement.getType()
                );
                registryStatement.markAsSoftDeleted();
                modifiedRegistryStatements.add(registryStatement);
            }
        });
        statementRepository.saveAll(modifiedRegistryStatements);
        return modifiedRegistryStatements.size();
    }

    @Transactional
    public int resolveV1Statements() {
        var registryV1Statements = datastoreEntityRepository.findAllByStatus(DatastoreStatus.ACTIVE);
        var modifiedRegistryV1Statements = new ArrayList<DatastoreEntity>();
        registryV1Statements.forEach(registryV1Statement -> {
            if (
                !trustStatementPartnerLinkRepository.existsByIdOrTrustRegistryEntryId(
                    registryV1Statement.getId(),
                    registryV1Statement.getId()
                )
            ) {
                log.warn(
                    "LostStatementsIdentification: Statement {} exists in registry DB but not management DB. Marking as DEACTIVATED.",
                    registryV1Statement.getId()
                );
                registryV1Statement.changeStatus(DatastoreStatus.DEACTIVATED);
                modifiedRegistryV1Statements.add(registryV1Statement);
            }
        });
        datastoreEntityRepository.saveAll(modifiedRegistryV1Statements);
        return modifiedRegistryV1Statements.size();
    }
}
