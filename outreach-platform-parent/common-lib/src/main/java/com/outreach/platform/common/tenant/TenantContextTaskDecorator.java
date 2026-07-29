package com.outreach.platform.common.tenant;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.UUID;

/**
 * A {@link TaskDecorator} that propagates the {@link TenantContext} and SLF4J MDC
 * from the calling thread to the executing thread in asynchronous task execution.
 * <p>
 * When a task is submitted to a {@code ThreadPoolTaskExecutor}, the calling thread's
 * tenant identity and MDC diagnostic context are captured at decoration time. The
 * executing (child) thread then inherits this context before the task runs, and the
 * context is cleared in a {@code finally} block to prevent thread-local leakage in
 * pooled-thread environments.
 * <p>
 * If no tenant context is present on the calling thread, the original runnable is
 * returned unmodified — no context manipulation occurs.
 * <p>
 * <strong>Usage:</strong> Register this decorator on {@code ThreadPoolTaskExecutor}
 * beans (including the default {@code @Async} executor) to ensure tenant isolation
 * is maintained across asynchronous boundaries.
 *
 * <pre>{@code
 * @Bean
 * public ThreadPoolTaskExecutor taskExecutor() {
 *     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
 *     executor.setTaskDecorator(new TenantContextTaskDecorator());
 *     executor.initialize();
 *     return executor;
 * }
 * }</pre>
 *
 * @see TenantContext
 * @see org.springframework.core.task.TaskDecorator
 */
public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture tenant context from the calling thread at decoration time
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        // If no tenant context is set, run the original task unmodified
        if (tenantId == null) {
            return runnable;
        }

        return () -> {
            try {
                // Restore tenant context on the executing thread
                TenantContext.setCurrentTenantId(tenantId);

                // Restore MDC context on the executing thread
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }

                runnable.run();
            } finally {
                // Clear both contexts to prevent thread-local leakage
                TenantContext.clear();
                MDC.clear();
            }
        };
    }
}
