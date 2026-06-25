package ch.admin.bj.swiyu.trust.management.test;

import ch.admin.bj.swiyu.trust.management.modules.common.async.AsyncConfig;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
@Configuration
@Import(AsyncConfig.class)
@RequiredArgsConstructor
public class AsyncTestConfig {

    private final ThreadPoolTaskExecutor applicationTaskExecutor;

    /**
     * Some work is done Through @TransactionalEventHandler and @Async. In order to test the results we
     * need to wait until they are done after an operation is finished.
     */
    public void waitForAsyncOperationsFinished() {
        try {
            this.applicationTaskExecutor.getThreadPoolExecutor().awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IllegalStateException(
                "Eine Asynchrone Operation in einem Integration Test hat länger als der maximale Wert von 2 Sekunden gedauert. Falls das wiederholt der Fall sein sollte, kann der Wert hier erhöht werden.",
                e
            );
        }
    }
}
