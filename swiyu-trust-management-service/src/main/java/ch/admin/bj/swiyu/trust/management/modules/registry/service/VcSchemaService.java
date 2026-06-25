package ch.admin.bj.swiyu.trust.management.modules.registry.service;

import static ch.admin.bj.swiyu.trust.management.modules.common.persistence.TransactionManagerNames.REGISTRY_TRANSACTION_MANAGER;

import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaAlreadyExistsException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaPublicationFailedException;
import ch.admin.bj.swiyu.trust.management.modules.common.exception.VcSchemaUrlValidationFailedException;
import ch.admin.bj.swiyu.trust.management.modules.common.registry.VcSchemaUrlValidator;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.VcSchemaDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.VcSchemaStatusDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcSchema;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.VcSchemaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class VcSchemaService {

    private final VcSchemaRepository vcSchemaRepository;
    private final VcSchemaUrlValidator vcSchemaUrlValidator;
    private final ObjectMapper objectMapper;

    @Transactional(transactionManager = REGISTRY_TRANSACTION_MANAGER)
    public VcSchemaDto publishVcSchema(String file)
        throws VcSchemaUrlValidationFailedException, VcSchemaAlreadyExistsException {
        String uniquePathSegment = getVctResourcePath(file);
        var vcSchema = vcSchemaRepository.findByPath(uniquePathSegment);
        if (vcSchema.isPresent()) {
            log.info("VC Schema with path {} already exists", uniquePathSegment);
            throw new VcSchemaAlreadyExistsException("VC Schema with path " + uniquePathSegment + " already exists");
        }
        log.info("Creating new VC Schema with path {}", uniquePathSegment);
        var newSchema = vcSchemaRepository.save(new VcSchema(uniquePathSegment, file));
        return new VcSchemaDto(
            newSchema.getId(),
            newSchema.getFile(),
            newSchema.getPath(),
            VcSchemaStatusDto.valueOf(newSchema.getStatus().name())
        );
    }

    /*
     * Extracts the user provided last path segment of the vct URL which served as unique identifier
     */
    private String getVctResourcePath(String file)
        throws VcSchemaPublicationFailedException, VcSchemaUrlValidationFailedException {
        JsonNode vcSchema;
        try {
            vcSchema = objectMapper.readTree(file);
        } catch (JsonProcessingException e) {
            throw new VcSchemaPublicationFailedException("File of vc schema submission is invalid json");
        }
        String vct;
        try {
            vct = vcSchema.get("vct").asText();
        } catch (NullPointerException e) {
            throw new VcSchemaPublicationFailedException("File of vc schema submission doesn't contain vct property");
        }
        return vcSchemaUrlValidator.getVctResourcePath(vct); // This will throw an exception if the URL is invalid
    }
}
