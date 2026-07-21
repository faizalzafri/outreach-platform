package com.outreach.platform.report.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * RabbitMQ listener for domain events consumed by the report service.
 * Bridges RabbitMQ messages to local Spring ApplicationEvents, so existing
 * listeners like {@link ReportCacheInvalidationListener} continue to work.
 */
@Service
public class RabbitMqEventListener {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventListener.class);

    private final ApplicationEventPublisher applicationEventPublisher;

    @Inject
    public RabbitMqEventListener(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Listens for ImportJobCompleted events on the report queue and publishes
     * a local Spring ApplicationEvent to trigger cache invalidation.
     */
    @RabbitListener(queues = RabbitMqConstants.QUEUE_REPORT)
    public void onImportJobCompleted(DomainEventMessage message) {
        log.info("Received ImportJobCompleted event via RabbitMQ: eventId={}", message.eventId());

        String jobId = String.valueOf(message.payload().getOrDefault("jobId", message.eventId()));

        applicationEventPublisher.publishEvent(new ImportJobCompletedEvent(jobId));

        log.debug("Published local ImportJobCompletedEvent for jobId={}", jobId);
    }
}
