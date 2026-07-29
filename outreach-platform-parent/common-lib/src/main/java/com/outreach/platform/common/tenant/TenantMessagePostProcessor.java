package com.outreach.platform.common.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

/**
 * RabbitMQ {@link MessagePostProcessor} that propagates the current tenant identity
 * to outbound messages via the {@code x-tenant-id} header.
 * <p>
 * When a tenant context is present (see {@link TenantContext#isPresent()}), this
 * post-processor adds the tenant UUID as a string header on the AMQP message.
 * Downstream consumers can then extract this header to restore tenant context
 * before processing the message.
 * <p>
 * If no tenant context is set (e.g., for system-level messages that are not scoped
 * to a specific tenant), a warning is logged but message sending is not blocked.
 *
 * @see TenantContext
 * @see TenantConstants#X_TENANT_ID_HEADER
 */
public class TenantMessagePostProcessor implements MessagePostProcessor {

    private static final Logger log = LoggerFactory.getLogger(TenantMessagePostProcessor.class);

    /**
     * AMQP header name used to propagate tenant identity between services via RabbitMQ.
     */
    static final String TENANT_ID_HEADER = "x-tenant-id";

    @Override
    public Message postProcessMessage(Message message) {
        if (TenantContext.isPresent()) {
            message.getMessageProperties().setHeader(
                    TENANT_ID_HEADER,
                    TenantContext.getCurrentTenantId().toString()
            );
        } else {
            log.warn("No tenant context available when publishing message. "
                    + "Outbound message will not carry x-tenant-id header. "
                    + "This is expected for system-level messages but may indicate a bug for tenant-scoped operations.");
        }
        return message;
    }
}
