package com.outreach.platform.ingestion.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

/**
 * Feign client for inter-service calls to Event Service.
 * Used by Ingestion Service to create/update events and volunteers after file import.
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
     * Create a new event.
     */
    @PostMapping("/events")
    EventResponse createEvent(@RequestBody EventCreateRequest request);

    /**
     * Enroll volunteers in an event.
     */
    @PostMapping("/events/{eventId}/volunteers")
    List<EnrollmentResponse> enrollVolunteers(
            @PathVariable("eventId") UUID eventId,
            @RequestBody VolunteerEnrollRequest request
    );

    /**
     * Get a volunteer profile by employee ID.
     */
    @GetMapping("/volunteers/{employeeId}")
    VolunteerResponse getVolunteerProfile(@PathVariable("employeeId") String employeeId);

    /**
     * Upserts a volunteer profile by employeeId and enrolls it in the event identified by
     * eventCode. Idempotent: re-importing an already-enrolled volunteer is a no-op.
     */
    @PostMapping("/volunteers/import")
    VolunteerImportResponse importVolunteer(@RequestBody VolunteerImportRequest request);

    // --- Request/Response DTOs ---

    record EventCreateRequest(
            String eventCode,
            String eventName,
            String description,
            String eventDate,
            String eventEndDate,
            String city,
            String venue,
            String category,
            Integer maxVolunteers
    ) {}

    record VolunteerEnrollRequest(
            List<String> employeeIds
    ) {}

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
            String emailStatus
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

    record VolunteerImportRequest(
            String employeeId,
            String fullName,
            String email,
            String phone,
            String baseLocation,
            String department,
            String designation,
            String skills,
            String eventCode
    ) {}

    record VolunteerImportResponse(
            UUID volunteerId,
            UUID eventId,
            boolean alreadyEnrolled
    ) {}
}
