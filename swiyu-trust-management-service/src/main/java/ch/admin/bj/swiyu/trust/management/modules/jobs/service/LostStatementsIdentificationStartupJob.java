package ch.admin.bj.swiyu.trust.management.modules.jobs.service;

import ch.admin.bj.swiyu.trust.management.modules.common.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LostStatementsIdentificationStartupJob {

    private final AsyncService async;
    private final LostStatementsIdentificationStartupService lostStatementsIdentificationStartupService;

    @EventListener(ApplicationReadyEvent.class)
    public void loadTestData() {
        async.run(() -> {
            try {
                log.info(
                    "LostStatementsIdentification: Identifying statements which exists in registry, but not management DB!"
                );

                var modifiedStatementCount = lostStatementsIdentificationStartupService.resolveV2Statements();
                modifiedStatementCount += lostStatementsIdentificationStartupService.resolveV1Statements();

                if (modifiedStatementCount > 0) {
                    log.error("LostStatementsIdentification: done! Edited {} statements.", modifiedStatementCount);
                } else {
                    log.info("LostStatementsIdentification: done! Edited {} statements.", modifiedStatementCount);
                }
            } catch (Exception e) {
                log.error("LostStatementsIdentification: failed!", e);
            }
        });
    }
}
