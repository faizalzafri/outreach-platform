package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.EnrollmentDto;
import com.outreach.platform.event.model.dto.VolunteerEnrollRequest;
import com.outreach.platform.event.service.VolunteerEnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for volunteer enrollment in events.
 * Delegates entirely to {@link VolunteerEnrollmentService}.
 */
@RestController
@RequestMapping("/events/{eventId}/volunteers")
@Tag(name = "Volunteer Enrollment", description = "Enroll, list, and remove volunteers enrolled in an event")
public class VolunteerEnrollmentController {

    private static final String PMO_OR_ADMIN =
            "hasAnyRole('PMO', 'ADMIN', 'TENANT_ADMIN', 'PLATFORM_ADMIN')";

    private final VolunteerEnrollmentService enrollmentService;

    @Inject
    public VolunteerEnrollmentController(VolunteerEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(summary = "List enrolled volunteers", description = "Lists all volunteers enrolled in an event")
    @GetMapping
    public ResponseEntity<List<EnrollmentDto>> listEnrolledVolunteers(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId) {
        return ResponseEntity.ok(enrollmentService.listEnrolledVolunteers(eventId));
    }

    @Operation(summary = "Enroll volunteers", description = "Enrolls one or more volunteers in an event by employee ID")
    @PreAuthorize(PMO_OR_ADMIN)
    @PostMapping
    public ResponseEntity<List<EnrollmentDto>> enrollVolunteers(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId,
            @Valid @RequestBody VolunteerEnrollRequest request) {
        List<EnrollmentDto> enrolled = enrollmentService.enrollVolunteers(eventId, request.employeeIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(enrolled);
    }

    @Operation(summary = "Remove volunteer enrollment", description = "Removes a volunteer's enrollment from an event")
    @PreAuthorize(PMO_OR_ADMIN)
    @DeleteMapping("/{employeeId}")
    public ResponseEntity<Void> removeVolunteer(
            @Parameter(description = "Event UUID") @PathVariable UUID eventId,
            @Parameter(description = "Employee ID") @PathVariable String employeeId) {
        enrollmentService.removeVolunteer(eventId, employeeId);
        return ResponseEntity.noContent().build();
    }
}
