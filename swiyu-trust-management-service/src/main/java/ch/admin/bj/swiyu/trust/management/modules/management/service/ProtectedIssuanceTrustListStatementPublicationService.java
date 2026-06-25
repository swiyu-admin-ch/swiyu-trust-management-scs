package ch.admin.bj.swiyu.trust.management.modules.management.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProtectedIssuanceTrustListStatementPublicationService {

    private final TrustStatementService trustStatementService;

    @Async
    public void triggerPublicationAsync() {
        log.debug("Triggering Protected Issuance Trust List Statement publication...");
        trustStatementService.issueAndPublishProtectedIssuanceV2TrustListStatement();
        log.info("Successfully published Protected Issuance Trust List Statement");
    }
}
