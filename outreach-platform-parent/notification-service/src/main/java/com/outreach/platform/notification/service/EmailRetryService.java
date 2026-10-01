package com.outreach.platform.notification.service;

import com.outreach.platform.notification.config.NotificationServiceProperties;
import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Retries failed email deliveries based on scheduled retry times or manual triggers. */
@Service
public class EmailRetryService {

    private static final Logger log = LoggerFactory.getLogger(EmailRetryService.class);

    private final EmailDeliveryRepository deliveryRepository;
    private final EmailDispatchService emailDispatchService;
    private final NotificationServiceProperties properties;

    @Inject
    public EmailRetryService(EmailDeliveryRepository deliveryRepository,
                             EmailDispatchService emailDispatchService,
                             NotificationServiceProperties properties) {
        this.deliveryRepository = deliveryRepository;
        this.emailDispatchService = emailDispatchService;
        this.properties = properties;
    }

    /**
     * Re-dispatches all FAILED deliveries whose nextRetryAt has passed. Scheduled so the
     * retry-backoff-minutes schedule EmailDispatchService writes into nextRetryAt is acted on.
     */
    @Scheduled(initialDelayString = "${notification-service.retry-poll-interval-ms:60000}",
            fixedDelayString = "${notification-service.retry-poll-interval-ms:60000}")
    public int retryDueDeliveries() {
        List<EmailDeliveryDocument> dueForRetry =
                deliveryRepository.findByStatusAndNextRetryAtBefore(DeliveryStatus.FAILED, Instant.now());

        if (!dueForRetry.isEmpty()) {
            log.info("Found {} deliveries due for retry", dueForRetry.size());
        }

        for (EmailDeliveryDocument delivery : dueForRetry) {
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextRetryAt(null);
            deliveryRepository.save(delivery);
            emailDispatchService.dispatchEmail(delivery);
        }

        return dueForRetry.size();
    }

    /** Manually retries all failed deliveries for a given event, resetting attempts if permanently failed. */
    public int retryFailedForEvent(String eventId) {
        List<EmailDeliveryDocument> allFailed =
                new ArrayList<>(deliveryRepository.findByEventIdAndStatus(eventId, DeliveryStatus.FAILED));
        allFailed.addAll(deliveryRepository.findByEventIdAndStatus(eventId, DeliveryStatus.PERMANENTLY_FAILED));

        log.info("Manual retry requested for event {}. Found {} failed deliveries", eventId, allFailed.size());

        for (EmailDeliveryDocument delivery : allFailed) {
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextRetryAt(null);
            delivery.setErrorMessage(null);
            if (delivery.getAttempts() >= properties.maxRetryAttempts()) {
                delivery.setAttempts(0);
            }
            deliveryRepository.save(delivery);
            emailDispatchService.dispatchEmail(delivery);
        }

        return allFailed.size();
    }
}
