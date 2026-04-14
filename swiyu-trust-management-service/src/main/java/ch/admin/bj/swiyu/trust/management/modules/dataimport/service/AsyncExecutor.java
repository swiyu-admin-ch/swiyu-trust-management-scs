package ch.admin.bj.swiyu.trust.management.modules.dataimport.service;

import ch.admin.bj.swiyu.trust.management.modules.common.async.*;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.boot.context.event.*;
import org.springframework.context.annotation.*;
import org.springframework.context.event.*;
import org.springframework.stereotype.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Profile("test-data-injection")
public class AsyncExecutor {

    private final AsyncService async;
    private final DemoDataImportService demoDataImportService;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTestData() {
        async.run(() -> {
            try {
                log.warn("LOCAL TEST DATA INJECTION is happening!");
                // setting system auth for AuditMetadata
                demoDataImportService.setSystemSecurityContext();

                demoDataImportService.deleteTrustOnboardingTasks();
                demoDataImportService.loadTrustOnboardingTasks();

                log.debug("LOCAL TEST DATA INJECTION is done!");
            } catch (Exception e) {
                log.error("LOCAL TEST DATA INJECTION failed!", e);
            }
        });
    }
}
