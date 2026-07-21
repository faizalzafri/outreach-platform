package com.outreach.platform.notification.service;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.model.dto.DeliveryAnalyticsResponse;
import com.outreach.platform.notification.model.dto.DeliveryStatusSummary;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides delivery status queries, history lookups, and analytics for email deliveries.
 */
@Service
public class DeliveryTrackingService {

    private final EmailDeliveryRepository deliveryRepository;

    @Inject
    public DeliveryTrackingService(EmailDeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    /**
     * Returns a status summary for all deliveries associated with an event.
     */
    public DeliveryStatusSummary getStatusSummary(String eventId) {
        List<EmailDeliveryDocument> deliveries = deliveryRepository.findByEventId(eventId);
        long total = deliveries.size();
        long pending = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.PENDING).count();
        long queued = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.QUEUED).count();
        long sent = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.SENT).count();
        long delivered = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.DELIVERED).count();
        long bounced = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.BOUNCED).count();
        long failed = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.FAILED).count();
        long permanentlyFailed = deliveries.stream().filter(d -> d.getStatus() == DeliveryStatus.PERMANENTLY_FAILED).count();

        return new DeliveryStatusSummary(eventId, total, pending, queued, sent, delivered, bounced, failed, permanentlyFailed);
    }

    /**
     * Returns paginated delivery history across all events.
     */
    public Page<EmailDeliveryDocument> getDeliveryHistory(Pageable pageable) {
        return deliveryRepository.findAll(pageable);
    }

    /**
     * Returns paginated delivery history for a specific event.
     */
    public Page<EmailDeliveryDocument> getDeliveryHistoryByEvent(String eventId, Pageable pageable) {
        return deliveryRepository.findByEventId(eventId, pageable);
    }

    /**
     * Computes delivery rate analytics: percentage of sent, failed, bounced, and delivered emails.
     */
    public DeliveryAnalyticsResponse getAnalytics() {
        long total = deliveryRepository.count();
        if (total == 0) {
            return new DeliveryAnalyticsResponse(0, 0, 0, 0, 0, 0);
        }

        long sent = deliveryRepository.countByStatus(DeliveryStatus.SENT);
        long delivered = deliveryRepository.countByStatus(DeliveryStatus.DELIVERED);
        long bounced = deliveryRepository.countByStatus(DeliveryStatus.BOUNCED);
        long failed = deliveryRepository.countByStatus(DeliveryStatus.FAILED);
        long permanentlyFailed = deliveryRepository.countByStatus(DeliveryStatus.PERMANENTLY_FAILED);

        double sentRate = (double) sent / total * 100;
        double deliveredRate = (double) delivered / total * 100;
        double bouncedRate = (double) bounced / total * 100;
        double failedRate = (double) (failed + permanentlyFailed) / total * 100;

        return new DeliveryAnalyticsResponse(total, sentRate, deliveredRate, bouncedRate, failedRate, permanentlyFailed);
    }
}
