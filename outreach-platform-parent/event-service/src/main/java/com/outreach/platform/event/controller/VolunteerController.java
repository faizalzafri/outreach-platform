package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.VolunteerDto;
import com.outreach.platform.event.model.dto.VolunteerHistoryDto;
import com.outreach.platform.event.model.dto.VolunteerProfileUpdateRequest;
import com.outreach.platform.event.service.VolunteerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the volunteer directory: listing/searching, profile detail,
 * profile/availability updates, and per-volunteer participation history.
 */
@RestController
@RequestMapping("/volunteers")
@Tag(name = "Volunteers", description = "Volunteer directory: listing, profile detail, and participation history")
public class VolunteerController {

    private static final String PMO_OR_ADMIN =
            "hasAnyRole('PMO', 'ADMIN', 'TENANT_ADMIN', 'PLATFORM_ADMIN')";

    private final VolunteerService volunteerService;

    @Inject
    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @Operation(summary = "List volunteers", description = "Lists volunteers, optionally searching by skills, location, or department")
    @GetMapping
    public ResponseEntity<Page<VolunteerDto>> listVolunteers(
            @Parameter(description = "Free-text search across skills, base location, and department")
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(volunteerService.listVolunteers(search, pageable));
    }

    @Operation(summary = "Get volunteer profile", description = "Retrieves a volunteer's profile by employee ID")
    @GetMapping("/{employeeId}")
    public ResponseEntity<VolunteerDto> getVolunteer(
            @Parameter(description = "Employee ID") @PathVariable String employeeId) {
        return ResponseEntity.ok(volunteerService.getVolunteer(employeeId));
    }

    @Operation(summary = "Update volunteer profile", description = "Updates a volunteer's profile fields, including availability. All fields optional.")
    @PreAuthorize(PMO_OR_ADMIN)
    @PutMapping("/{employeeId}")
    public ResponseEntity<VolunteerDto> updateProfile(
            @Parameter(description = "Employee ID") @PathVariable String employeeId,
            @Valid @RequestBody VolunteerProfileUpdateRequest request) {
        return ResponseEntity.ok(volunteerService.updateProfile(employeeId, request));
    }

    @Operation(summary = "Get participation history", description = "Lists the events a volunteer has enrolled in, paginated")
    @GetMapping("/{employeeId}/history")
    public ResponseEntity<Page<VolunteerHistoryDto>> getHistory(
            @Parameter(description = "Employee ID") @PathVariable String employeeId,
            Pageable pageable) {
        return ResponseEntity.ok(volunteerService.getHistory(employeeId, pageable));
    }
}
