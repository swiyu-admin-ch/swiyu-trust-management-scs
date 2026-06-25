package ch.admin.bj.swiyu.trust.management.modules.management.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NonCompliantActorPublicationService {

    private final TrustStatementService trustStatementService;
    private final NonCompliantActorService nonCompliantActorService;

    @Async
    // no @Transactional as V1 and V2 should be handled in their own transactions
    public void triggerPublicationAsync() {
        log.debug("Triggering non-compliant actor publication...");
        // publish V1 of non-Compliance list
        nonCompliantActorService.issueAndPublishNonComplianceV1();
        // publish V2 of ncTLS according to TP2.0
        trustStatementService.issueAndPublishNonComplianceV2TrustListStatement();
        log.info("Successfully published non-compliance-list");
    }

    // no @Transactional as V1 and V2 should be handled in their own transactions
    public void triggerPublication() {
        log.debug("Triggering non-compliant actor publication...");
        // publish V1 of non-Compliance list
        nonCompliantActorService.issueAndPublishNonComplianceV1();
        // publish V2 of ncTLS according to TP2.0
        trustStatementService.issueAndPublishNonComplianceV2TrustListStatement();
        log.info("Successfully published non-compliance-list");
    }
}
