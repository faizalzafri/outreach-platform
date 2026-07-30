package com.outreach.platform.common.tenant;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;
import java.util.UUID;

/** TaskDecorator that propagates TenantContext and MDC from the calling thread to async executor threads. */
public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        if (tenantId == null) {
            return runnable;
        }

        return () -> {
            try {
                TenantContext.setCurrentTenantId(tenantId);
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                runnable.run();
            } finally {
                TenantContext.clear();
                MDC.clear();
            }
        };
    }
}
