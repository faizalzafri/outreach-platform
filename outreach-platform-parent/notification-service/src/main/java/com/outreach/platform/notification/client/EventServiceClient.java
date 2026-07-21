package com.outreach.platform.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for inter-service calls to Event Service.
 * Used by Notification Service to retrieve event details and volunteer enrollment info.
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
    EventServiceClient.EventResponse getEvent(@PathVariable("eventId") UUID eventId);

    /**
     * List enrolled volunteers for an event.
     */
    @GetMapping("/events/{eventId}/volunteers")
    List<EventServiceClient.EnrollmentResponse> getEventVolunteers(@PathVariable("eventId") UUID eventId);

    /**
     * Get a volunteer profile by employee ID.
     */
    @GetMapping("/events/volunteers/{employeeId}/profile")
    EventServiceClient.VolunteerResponse getVolunteerProfile(@PathVariable("employeeId") String employeeId);

    // --- Response DTOs (local copies to avoid cross-module dependency) ---

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
            Integer attendedCount
    ) {}

    record EnrollmentResponse(
            UUID id,
            UUID eventId,
            UUID volunteerId,
            String employeeId,
            String volunteerName,
            String attendanceStatus,
            String emailStatus,
            String registeredAt,
            String attendanceMarkedAt
    ) {}

    record VolunteerResponse(
            UUID id,
            String employeeId,
            String fullName,
            String email,
            String phone,
            String baseLocation,
            String department,
            String designation,
            String skills,
            String availability,
            Integer totalEventsParticipated
    ) {}
}
