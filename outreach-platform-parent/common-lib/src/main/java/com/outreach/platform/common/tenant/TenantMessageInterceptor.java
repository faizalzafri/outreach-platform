package com.outreach.platform.common.tenant;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;

import java.util.UUID;

/** MethodInterceptor that extracts x-tenant-id from inbound RabbitMQ messages and populates TenantContext. */
public class TenantMessageInterceptor implements MethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantMessageInterceptor.class);

    private static final String TENANT_HEADER = "x-tenant-id";
    private static final String MDC_TENANT_KEY = "tenant_id";

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Message message = extractMessage(invocation);

        if (message == null) {
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

    private Message extractMessage(MethodInvocation invocation) {
        for (Object arg : invocation.getArguments()) {
            if (arg instanceof Message msg) {
                return msg;
            }
        }
        return null;
    }
}
