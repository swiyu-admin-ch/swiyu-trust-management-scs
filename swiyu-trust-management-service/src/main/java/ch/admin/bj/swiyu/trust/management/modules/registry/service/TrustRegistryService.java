/*
 * SPDX-FileCopyrightText: 2025 Swiss Confederation
 *
 * SPDX-License-Identifier: MIT
 */

package ch.admin.bj.swiyu.trust.management.modules.registry.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.REGISTRY_TRANSACTION_MANAGER;
import static ch.admin.bj.swiyu.trust.management.modules.registry.domain.DatastoreStatus.DEACTIVATED;
import static ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryMapper.toDatastoreEntityResponseDto;
import static ch.admin.bj.swiyu.trust.management.modules.registry.service.TrustRegistryMapper.toDatastoreStatusDto;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.ResourceNotFoundException;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.DatastoreStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.StatementDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.StatementTypeDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.*;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class TrustRegistryService {

    private final StatementRepository statementRepository;
    private final DatastoreEntityRepository datastoreEntityRepository;
    private final VcEntityRepository vcEntityRepository;
    private final JsonJwtDeserializer jsonJwtDeserializer;

    @Transactional(transactionManager = REGISTRY_TRANSACTION_MANAGER)
    public StatementDto createStatement(StatementTypeDto type, UUID id, String serialized) {
        var statement = statementRepository.save(
            new Statement(
                id,
                false,
                true,
                serialized,
                jsonJwtDeserializer.decodeJwt(serialized),
                TrustRegistryMapper.toStatementType(type)
            )
        );
        log.info("created new statement with id: {}", statement.getId());
        return TrustRegistryMapper.toStatementDto(statement);
    }

    @Transactional(readOnly = true, transactionManager = REGISTRY_TRANSACTION_MANAGER)
    public StatementDto getStatement(UUID id) {
        return TrustRegistryMapper.toStatementDto(geStatement(id));
    }

    @Transactional(transactionManager = REGISTRY_TRANSACTION_MANAGER)
    public DatastoreDto createTrustStatementVc(String rawVc) {
        var datastoreEntity = datastoreEntityRepository.save(new DatastoreEntity());
        log.info("created datastore entry with id: {}", datastoreEntity.getId());
        return updateTrustStatementVc(rawVc, datastoreEntity);
    }

    @Transactional(readOnly = true, transactionManager = REGISTRY_TRANSACTION_MANAGER)
    public DatastoreStatusDto getStatus(UUID datastoreEntityId) {
        var datastoreEntity = getDatastoreEntity(datastoreEntityId);
        return toDatastoreStatusDto(datastoreEntity.getStatus());
    }

    @Transactional(transactionManager = REGISTRY_TRANSACTION_MANAGER)
    public void markAsNonActiveInStatuslist(UUID statementId) {
        var statement = geStatement(statementId);
        statement.markAsInactiveInStatuslist();
        statementRepository.save(statement);
    }

    @Transactional(transactionManager = REGISTRY_TRANSACTION_MANAGER)
    public void deactivate(UUID datastoreEntityId) {
        var entity = getDatastoreEntity(datastoreEntityId);
        entity.changeStatus(DEACTIVATED);
        datastoreEntityRepository.save(entity);
    }

    @Transactional(transactionManager = REGISTRY_TRANSACTION_MANAGER)
    public DatastoreDto updateTrustStatementVc(UUID datastoreEntityId, String rawVc) {
        var datastoreEntity = getDatastoreEntity(datastoreEntityId);
        return updateTrustStatementVc(rawVc, datastoreEntity);
    }

    private static void validateCanEdit(DatastoreEntity entry) {
        if (entry.getStatus() == DatastoreStatus.DISABLED) {
            throw new IllegalStateException(
                "Cannot edit trust registry entry with id " + entry.getId() + " since it is in status 'DISABLED'. "
            );
        }
    }

    private Statement geStatement(UUID id) {
        return statementRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("The statement  with id " + id + " does not exist."));
    }

    private DatastoreDto updateTrustStatementVc(String rawVc, DatastoreEntity datastoreEntity) {
        validateCanEdit(datastoreEntity);
        var vcPayload = jsonJwtDeserializer.decodeSdjwtPayload(rawVc);
        var existing = vcEntityRepository.findByDatastoreEntityId(datastoreEntity.getId());
        if (existing.isEmpty()) {
            var vcEntity = new VcEntity(datastoreEntity.getId(), rawVc, vcPayload);
            vcEntityRepository.save(vcEntity);
        } else {
            var vcEntity = existing.get();
            vcEntity.updateVc(rawVc, vcPayload);
            vcEntityRepository.save(vcEntity);
        }
        datastoreEntity.changeStatus(DatastoreStatus.ACTIVE);
        datastoreEntity = datastoreEntityRepository.save(datastoreEntity);
        log.info("Updated trust statement V1 for datastore entry with id: {}", datastoreEntity.getId());
        var vcEntity = vcEntityRepository.findByDatastoreEntityId(datastoreEntity.getId());
        return toDatastoreEntityResponseDto(datastoreEntity, vcEntity.orElseThrow());
    }

    private DatastoreEntity getDatastoreEntity(UUID id) {
        return datastoreEntityRepository
            .findById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("The datastore entity with id " + id + " does not exist.")
            );
    }
}
