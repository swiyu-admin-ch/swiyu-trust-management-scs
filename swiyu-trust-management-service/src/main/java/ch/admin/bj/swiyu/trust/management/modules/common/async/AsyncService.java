package ch.admin.bj.swiyu.trust.management.modules.common.async;

import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.*;

@Component
public class AsyncService {

    @Async
    public void run(final Runnable runnable) {
        runnable.run();
    }
}
