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
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.*;
import com.authlete.sd.Disclosure;
import com.authlete.sd.SDJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Base64;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class TrustRegistryService {

    private final DatastoreEntityRepository datastoreEntityRepository;
    private final VcEntityRepository vcEntityRepository;

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

    private DatastoreDto updateTrustStatementVc(String rawVc, DatastoreEntity datastoreEntity) {
        validateCanEdit(datastoreEntity);
        var vcPayload = decodeSdjwtPayload(rawVc);
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

    /**
     * Extracts the resolved payload of an SD-JWT as JSON String
     *
     * @param encodedSdjwt the encoded SD-JWT with all disclosures etc.
     * @return Returns the decoded json string of the sd jwt payload
     */
    private static JsonNode decodeSdjwtPayload(String encodedSdjwt) {
        // Create JSON Mapper for further JSON manipulation
        ObjectMapper mapper = new ObjectMapper();
        mapper
            .getFactory()
            .configure(com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII.mappedFeature(), true);

        // Parse into SD JWT library
        var token = SDJWT.parse(encodedSdjwt);

        // extract original payload of SD-JWT
        try {
            var rawPayload = new String(Base64.getDecoder().decode(token.getCredentialJwt().split("\\.")[1]));
            ObjectNode payloadJson = (ObjectNode) mapper.readTree(rawPayload);

            // expand original payload with disclosed values
            for (Disclosure d : token.getDisclosures()) {
                payloadJson.set(d.getClaimName(), mapper.readTree(d.getJson()).get(2));
            }

            return payloadJson;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to decode encoded SD-JWT payload", e);
        }
    }

    private static void validateCanEdit(DatastoreEntity entry) {
        if (entry.getStatus() == DatastoreStatus.DISABLED) {
            throw new IllegalStateException(
                "Cannot edit trust registry entry with id " + entry.getId() + " since it is in status 'DISABLED'. "
            );
        }
    }
}
