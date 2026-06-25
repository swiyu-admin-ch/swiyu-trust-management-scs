package ch.admin.bj.swiyu.trust.management.modules.registry.service;

import ch.admin.bj.swiyu.trust.management.modules.registry.api.NonCompliantActorsDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.NonComplianceList;
import ch.admin.bj.swiyu.trust.management.modules.registry.domain.NonComplianceListRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@AllArgsConstructor
public class NonComplianceListService {

    private final NonComplianceListRepository nonComplianceListRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createNonComplianceList(NonCompliantActorsDto nonCompliantActors) {
        var payload = toJsonPayload(nonCompliantActors);
        var nonComplianceList = new NonComplianceList(UUID.randomUUID(), Instant.now(), payload);
        this.nonComplianceListRepository.save(nonComplianceList);
    }

    private String toJsonPayload(NonCompliantActorsDto nonCompliantActors) {
        try {
            return this.objectMapper.writeValueAsString(nonCompliantActors);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize non-compliant actors", e);
        }
    }
}
