package com.outreach.platform.ingestion.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

/**
 * Feign client for inter-service calls to Event Service.
 * Used by Ingestion Service to upsert and enroll volunteers after file import.
 */
@FeignClient(
        name = "event-service",
        fallbackFactory = EventServiceClientFallbackFactory.class
)
public interface EventServiceClient {

    /**
     * Upserts a volunteer profile by employeeId and enrolls it in the event identified by
     * eventCode. Idempotent: re-importing an already-enrolled volunteer is a no-op.
     */
    @PostMapping("/volunteers/import")
    VolunteerImportResponse importVolunteer(@RequestBody VolunteerImportRequest request);

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
