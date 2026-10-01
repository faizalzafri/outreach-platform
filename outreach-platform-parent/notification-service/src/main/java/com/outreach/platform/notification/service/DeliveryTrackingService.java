package com.outreach.platform.notification.service;

import com.outreach.platform.common.tenant.TenantContext;
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
import java.util.UUID;

/**
 * Provides delivery status queries, history lookups, and analytics for email deliveries.
 *
 * <p>Every read is scoped to the caller's tenant; an empty TenantContext (PLATFORM_ADMIN) gets
 * the cross-tenant view. Deliveries are Mongo documents, so there's no Hibernate filter to lean on.
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
        UUID tenantId = TenantContext.getCurrentTenantId();
        List<EmailDeliveryDocument> deliveries = tenantId != null
                ? deliveryRepository.findByTenantIdAndEventId(tenantId, eventId)
                : deliveryRepository.findByEventId(eventId);
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
     * Returns paginated delivery history across all of the caller's events.
     */
    public Page<EmailDeliveryDocument> getDeliveryHistory(Pageable pageable) {
        UUID tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null
                ? deliveryRepository.findByTenantId(tenantId, pageable)
                : deliveryRepository.findAll(pageable);
    }

    /**
     * Computes delivery rate analytics: percentage of sent, failed, bounced, and delivered emails.
     */
    public DeliveryAnalyticsResponse getAnalytics() {
        UUID tenantId = TenantContext.getCurrentTenantId();
        long total = tenantId != null ? deliveryRepository.countByTenantId(tenantId) : deliveryRepository.count();
        if (total == 0) {
            return new DeliveryAnalyticsResponse(0, 0, 0, 0, 0, 0);
        }

        long sent = count(tenantId, DeliveryStatus.SENT);
        long delivered = count(tenantId, DeliveryStatus.DELIVERED);
        long bounced = count(tenantId, DeliveryStatus.BOUNCED);
        long failed = count(tenantId, DeliveryStatus.FAILED);
        long permanentlyFailed = count(tenantId, DeliveryStatus.PERMANENTLY_FAILED);

        double sentRate = (double) sent / total * 100;
        double deliveredRate = (double) delivered / total * 100;
        double bouncedRate = (double) bounced / total * 100;
        double failedRate = (double) (failed + permanentlyFailed) / total * 100;

        return new DeliveryAnalyticsResponse(total, sentRate, deliveredRate, bouncedRate, failedRate, permanentlyFailed);
    }

    private long count(UUID tenantId, DeliveryStatus status) {
        return tenantId != null
                ? deliveryRepository.countByTenantIdAndStatus(tenantId, status)
                : deliveryRepository.countByStatus(status);
    }
}
