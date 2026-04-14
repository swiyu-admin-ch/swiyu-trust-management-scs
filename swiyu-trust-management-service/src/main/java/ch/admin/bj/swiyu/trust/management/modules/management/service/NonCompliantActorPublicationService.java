package ch.admin.bj.swiyu.trust.management.modules.management.service;

import ch.admin.bj.swiyu.trust.management.modules.management.domain.NonCompliantActorRepository;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.NonCompliantActorDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.api.NonCompliantActorsDto;
import ch.admin.bj.swiyu.trust.management.modules.registry.service.NonComplianceListService;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NonCompliantActorPublicationService {

    private final NonCompliantActorRepository nonCompliantActorRepository;
    private final NonComplianceListService nonComplianceListService;

    @Async
    @Transactional
    public void triggerPublicationAsync() {
        log.debug("Triggering non-compliant actor publication...");
        var nonCompliantActors = this.nonCompliantActorRepository.findAll();
        var nonComplianceList = nonCompliantActors
            .stream()
            .map(nonCompliantActor -> {
                var reason = new HashMap<String, String>();
                reason.put("de", nonCompliantActor.getReason().getReasonDe());
                reason.put("fr", nonCompliantActor.getReason().getReasonFr());
                reason.put("it", nonCompliantActor.getReason().getReasonIt());
                reason.put("en", nonCompliantActor.getReason().getReasonEn());
                reason.put("rm", nonCompliantActor.getReason().getReasonRm());
                return new NonCompliantActorDto(
                    nonCompliantActor.getDid(),
                    nonCompliantActor.getFlaggedAsNonCompliantAt(),
                    reason
                );
            })
            .toList();
        this.nonComplianceListService.createNonComplianceList(new NonCompliantActorsDto(nonComplianceList));
        log.info("Sucessfully published non-compliance-list");
    }
}
