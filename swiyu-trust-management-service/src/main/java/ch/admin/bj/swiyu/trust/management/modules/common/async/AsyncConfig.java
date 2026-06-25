package ch.admin.bj.swiyu.trust.management.modules.common.async;

import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Slf4j
@Configuration
@EnableAsync // @Async: e.g. for bucket intialization on startup
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final ThreadPoolTaskExecutor defaultSpringBootAsyncExecutor;

    /**
     * Gibt einen Executor vom Typ DelegatingSecurityContextAsyncTaskExecutor zurück. Dieser stellt sicher, dass der
     * Security context "vererbt" wird beim Aufruf von Methoden via @Async.
     */
    @Override
    public Executor getAsyncExecutor() {
        var delegator = new DelegatingSecurityContextAsyncTaskExecutor(defaultSpringBootAsyncExecutor);
        // using this delegator unfortunately loses the observability delegation (async threads won't
        // have a trace id). To fix this, pass the delegator from spring again here.
        defaultSpringBootAsyncExecutor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        return delegator;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
            log.error("Async method {} threw an uncaught exception", method.getName(), throwable);
    }
}
