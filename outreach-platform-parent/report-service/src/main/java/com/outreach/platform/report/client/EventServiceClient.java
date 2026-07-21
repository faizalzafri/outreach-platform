package com.outreach.platform.report.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for inter-service calls to Event Service.
 * Used by Report Service to retrieve event metadata for report aggregation.
 */
@FeignClient(
        name = "event-service",
        fallbackFactory = EventServiceClientFallbackFactory.class
)
public interface EventServiceClient {

    /**
     * Get event details by ID.
     */
    @GetMapping("/events/{eventId}")
    EventResponse getEvent(@PathVariable("eventId") UUID eventId);

    /**
     * List events with pagination (for report-wide aggregation).
     */
    @GetMapping("/events")
    PagedEventResponse listEvents(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    );

    /**
     * List enrolled volunteers for an event.
     */
    @GetMapping("/events/{eventId}/volunteers")
    List<EnrollmentResponse> getEventVolunteers(@PathVariable("eventId") UUID eventId);

    // --- Response DTOs ---

    record EventResponse(
            UUID id,
            String eventCode,
            String eventName,
            String description,
            String status,
            String eventDate,
            String eventEndDate,
            String city,
            String venue,
            String category,
            Integer maxVolunteers,
            Integer registeredCount,
            Integer attendedCount,
            String createdAt,
            String createdBy
    ) {}

    record EnrollmentResponse(
            UUID id,
            UUID eventId,
            UUID volunteerId,
            String employeeId,
            String volunteerName,
            String attendanceStatus,
            String emailStatus
    ) {}

    record PagedEventResponse(
            List<EventResponse> content,
            int totalPages,
            long totalElements,
            int number,
            int size
    ) {}
}
