package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Handles retry logic for failed email deliveries.
 * Finds deliveries that are due for retry and re-dispatches them.
 */
@Service
public class EmailRetryService {

    private static final Logger log = LoggerFactory.getLogger(EmailRetryService.class);

    private final EmailDeliveryRepository deliveryRepository;
    private final EmailDispatchService emailDispatchService;

    @Inject
    public EmailRetryService(EmailDeliveryRepository deliveryRepository,
                             EmailDispatchService emailDispatchService) {
        this.deliveryRepository = deliveryRepository;
        this.emailDispatchService = emailDispatchService;
    }

    /**
     * Finds all FAILED deliveries whose nextRetryAt has passed and re-dispatches them.
     *
     * @return the number of deliveries queued for retry
     */
    public int retryDueDeliveries() {
        List<EmailDeliveryDocument> dueForRetry =
                deliveryRepository.findByStatusAndNextRetryAtBefore(DeliveryStatus.FAILED, Instant.now());

        log.info("Found {} deliveries due for retry", dueForRetry.size());

        for (EmailDeliveryDocument delivery : dueForRetry) {
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextRetryAt(null);
            deliveryRepository.save(delivery);
            emailDispatchService.dispatchEmail(delivery);
        }

        return dueForRetry.size();
    }

    /**
     * Manually retries all failed or permanently failed deliveries for a given event.
     * Resets attempt count for permanently failed deliveries to allow retries.
     *
     * @param eventId the event whose failed deliveries should be retried
     * @return the number of deliveries queued for retry
     */
    public int retryFailedForEvent(String eventId) {
        List<EmailDeliveryDocument> failed = deliveryRepository.findByEventIdAndStatus(eventId, DeliveryStatus.FAILED);
        List<EmailDeliveryDocument> permanentlyFailed = deliveryRepository.findByEventIdAndStatus(eventId, DeliveryStatus.PERMANENTLY_FAILED);

        List<EmailDeliveryDocument> allFailed = new java.util.ArrayList<>(failed);
        allFailed.addAll(permanentlyFailed);

        log.info("Manual retry requested for event {}. Found {} failed deliveries", eventId, allFailed.size());

        for (EmailDeliveryDocument delivery : allFailed) {
            delivery.setStatus(DeliveryStatus.PENDING);
            delivery.setNextRetryAt(null);
            delivery.setErrorMessage(null);
            if (delivery.getAttempts() >= 5) {
                delivery.setAttempts(0);
            }
            deliveryRepository.save(delivery);
            emailDispatchService.dispatchEmail(delivery);
        }

        return allFailed.size();
    }
}
