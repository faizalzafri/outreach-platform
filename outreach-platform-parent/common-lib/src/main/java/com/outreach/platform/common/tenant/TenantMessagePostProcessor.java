package com.outreach.platform.common.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;

/** RabbitMQ MessagePostProcessor that adds the x-tenant-id header to outbound messages. */
public class TenantMessagePostProcessor implements MessagePostProcessor {

    private static final Logger log = LoggerFactory.getLogger(TenantMessagePostProcessor.class);

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
