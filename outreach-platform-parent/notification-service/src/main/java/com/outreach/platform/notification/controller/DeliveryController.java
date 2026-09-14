package com.outreach.platform.notification.controller;

import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.model.dto.DeliveryAnalyticsResponse;
import com.outreach.platform.notification.model.dto.DeliveryStatusSummary;
import com.outreach.platform.notification.model.dto.RetryResponse;
import com.outreach.platform.notification.service.DeliveryTrackingService;
import com.outreach.platform.notification.service.EmailRetryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for email delivery status, retry operations, history, and analytics.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/notifications")
@Tag(name = "Email Delivery", description = "Email delivery status, retry operations, history, and analytics")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
public class DeliveryController {

    private final DeliveryTrackingService deliveryTrackingService;
    private final EmailRetryService emailRetryService;

    @Inject
    public DeliveryController(DeliveryTrackingService deliveryTrackingService,
                              EmailRetryService emailRetryService) {
        this.deliveryTrackingService = deliveryTrackingService;
        this.emailRetryService = emailRetryService;
    }

    /**
     * GET /notifications/status/{eventId} — delivery status summary for an event.
     */
    @GetMapping("/status/{eventId}")
    public DeliveryStatusSummary getDeliveryStatus(@PathVariable String eventId) {
        return deliveryTrackingService.getStatusSummary(eventId);
    }

    /**
     * POST /notifications/retry/{eventId} — retry all failed emails for an event.
     */
    @PostMapping("/retry/{eventId}")
    public ResponseEntity<RetryResponse> retryFailed(@PathVariable String eventId) {
        int retriedCount = emailRetryService.retryFailedForEvent(eventId);
        String message = retriedCount > 0
                ? "Queued " + retriedCount + " failed deliveries for retry"
                : "No failed deliveries found for event";
        return ResponseEntity.ok(new RetryResponse(eventId, retriedCount, message));
    }

    /**
     * GET /notifications/history — paginated delivery history across all events.
     */
    @GetMapping("/history")
    public Page<EmailDeliveryDocument> getDeliveryHistory(Pageable pageable) {
        return deliveryTrackingService.getDeliveryHistory(pageable);
    }

    /**
     * GET /notifications/analytics — delivery rate analytics (sent/failed/bounced percentages).
     */
    @GetMapping("/analytics")
    public DeliveryAnalyticsResponse getAnalytics() {
        return deliveryTrackingService.getAnalytics();
    }
}
