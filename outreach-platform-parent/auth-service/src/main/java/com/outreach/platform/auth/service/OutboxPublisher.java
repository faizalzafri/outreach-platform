package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.OutboxMessage;
import com.outreach.platform.auth.repo.OutboxMessageRepository;
import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.common.tenant.TenantContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Publishes pending outbox messages to the {@code outreach.events} exchange. Runs every second so
 * one-time passcodes arrive promptly. Without a RabbitMQ connection (tests, or Keycloak mode) the
 * messages simply stay pending.
 */
@Named
@ConditionalOnProperty(name = "auth-server.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final int BATCH_SIZE = 50;

    private final OutboxMessageRepository outbox;
    private final ObjectProvider<RabbitTemplate> rabbitTemplate;

    @Inject
    public OutboxPublisher(OutboxMessageRepository outbox, ObjectProvider<RabbitTemplate> rabbitTemplate) {
        this.outbox = outbox;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelayString = "${auth-server.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        RabbitTemplate template = rabbitTemplate.getIfAvailable();
        if (template == null) {
            return;
        }
        for (OutboxMessage message : outbox.findByStatusOrderByCreatedDate(
                OutboxMessage.PENDING, PageRequest.of(0, BATCH_SIZE))) {
            try {
                // The shared RabbitTemplate stamps x-tenant-id from the tenant context.
                TenantContext.setCurrentTenantId(message.getTenantId());
                template.convertAndSend(RabbitMqConstants.EXCHANGE_OUTREACH_EVENTS, message.getRoutingKey(),
                        new DomainEventMessage(message.getId().toString(), message.getRoutingKey(),
                                message.getPayload(), message.getCreatedDate()));
                message.setStatus(OutboxMessage.PUBLISHED);
                message.setPublishedAt(Instant.now());
            } catch (AmqpException e) {
                // Leave it pending; the next run retries. Stop this batch to keep ordering.
                log.warn("Outbox publish failed, will retry: id={}, routingKey={}", message.getId(),
                        message.getRoutingKey(), e);
                return;
            } finally {
                TenantContext.clear();
            }
        }
    }
}
