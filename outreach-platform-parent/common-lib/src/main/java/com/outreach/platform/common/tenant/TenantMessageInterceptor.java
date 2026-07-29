package com.outreach.platform.common.tenant;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

import java.util.UUID;

/**
 * AOP {@link MethodInterceptor} (around-advice) that intercepts inbound RabbitMQ message
 * processing to establish tenant context before handler execution.
 * <p>
 * This interceptor is applied to {@code SimpleRabbitListenerContainerFactory} as an advice
 * chain element. For every inbound message it:
 * <ol>
 *   <li>Extracts the {@code x-tenant-id} header from the message properties</li>
 *   <li>Validates it as a well-formed UUID</li>
 *   <li>Populates {@link TenantContext} and SLF4J MDC with the tenant ID</li>
 *   <li>Invokes the actual message handler</li>
 *   <li>Clears {@link TenantContext} and MDC in a {@code finally} block</li>
 * </ol>
 * <p>
 * If the {@code x-tenant-id} header is missing or contains an invalid UUID, the message is
 * rejected with an {@link AmqpRejectAndDontRequeueException}, routing it to the dead-letter
 * queue. A warning is logged to aid in diagnosing upstream configuration issues.
 *
 * @see TenantContext
 * @see TenantConstants#X_TENANT_ID_HEADER
 */
public class TenantMessageInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantMessageInterceptor.class);

    /**
     * RabbitMQ message header name for tenant identification.
     * Uses lowercase format as per AMQP header conventions.
     */
    private static final String TENANT_HEADER = "x-tenant-id";

    private static final String MDC_TENANT_KEY = "tenant_id";

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Message message = extractMessage(invocation);

        if (message == null) {
            // No Message argument found — proceed without tenant context setup.
            // This should not happen in a well-configured listener, but we fail safe.
            return invocation.proceed();
        }

        String tenantIdHeader = message.getMessageProperties().getHeader(TENANT_HEADER);

        if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
            log.warn("Inbound RabbitMQ message rejected: missing '{}' header. "
                    + "MessageId={}, ConsumerQueue={}",
                    TENANT_HEADER,
                    message.getMessageProperties().getMessageId(),
                    message.getMessageProperties().getConsumerQueue());
            throw new AmqpRejectAndDontRequeueException(
                    "Message rejected: missing required '" + TENANT_HEADER + "' header");
        }

        UUID tenantId = parseUuid(tenantIdHeader, message);

        try {
            TenantContext.setCurrentTenantId(tenantId);
            MDC.put(MDC_TENANT_KEY, tenantId.toString());
            return invocation.proceed();
        } finally {
            TenantContext.clear();
            MDC.remove(MDC_TENANT_KEY);
        }
    }

    /**
     * Parses the tenant ID string as a UUID.
     *
     * @param tenantIdHeader the raw header value
     * @param message        the inbound message (for logging context)
     * @return the parsed UUID
     * @throws AmqpRejectAndDontRequeueException if the value is not a valid UUID
     */
    private UUID parseUuid(String tenantIdHeader, Message message) {
        try {
            return UUID.fromString(tenantIdHeader);
        } catch (IllegalArgumentException e) {
            log.warn("Inbound RabbitMQ message rejected: invalid UUID in '{}' header. "
                    + "Value='{}', MessageId={}, ConsumerQueue={}",
                    TENANT_HEADER,
                    tenantIdHeader,
                    message.getMessageProperties().getMessageId(),
                    message.getMessageProperties().getConsumerQueue());
            throw new AmqpRejectAndDontRequeueException(
                    "Message rejected: '" + TENANT_HEADER + "' header is not a valid UUID: "
                    + tenantIdHeader, e);
        }
    }

    /**
     * Scans the method invocation arguments to find the first {@link Message} parameter.
     * <p>
     * RabbitMQ listener methods always receive the raw {@link Message} as one of their
     * arguments (either explicitly declared or injected by the container).
     *
     * @param invocation the method invocation
     * @return the {@link Message} argument, or {@code null} if not found
     */
    private Message extractMessage(MethodInvocation invocation) {
        for (Object arg : invocation.getArguments()) {
            if (arg instanceof Message msg) {
                return msg;
            }
        }
        return null;
    }
}
