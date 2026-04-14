package ch.admin.bj.swiyu.trust.management.modules.common.async;

import java.util.concurrent.*;
import lombok.*;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.*;
import org.springframework.security.task.*;

@Configuration
@EnableAsync // @Async: e.g. for bucket intialization on startup
@EnableScheduling // @Scheduled: e.g. for checking pending isabv notifications
@AllArgsConstructor
public class AsyncConfiguration implements AsyncConfigurer {

    private final ThreadPoolTaskExecutor defaultSpringBootAsyncExecutor;

    /**
     * Gibt einen Executor vom Typ DelegatingSecurityContextAsyncTaskExecutor zurück. Dieser stellt sicher, dass der
     * Security context "vererbt" wird beim Aufruf von Methoden via @Async.
     */
    @Override
    public Executor getAsyncExecutor() {
        return new DelegatingSecurityContextAsyncTaskExecutor(defaultSpringBootAsyncExecutor);
    }
}
