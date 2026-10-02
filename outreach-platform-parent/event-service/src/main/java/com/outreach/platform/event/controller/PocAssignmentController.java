package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.PocAssignRequest;
import com.outreach.platform.event.model.dto.PocAssignmentDto;
import com.outreach.platform.event.service.PocAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST controller for assigning Points of Contact (POCs) to events. */
@RestController
@RequestMapping("/events")
@Tag(name = "POC Assignments", description = "Assign and remove an event's Points of Contact")
@PreAuthorize("hasAnyRole('PMO', 'ADMIN', 'TENANT_ADMIN', 'PLATFORM_ADMIN')")
public class PocAssignmentController {

    private final PocAssignmentService pocAssignmentService;

    @Inject
    public PocAssignmentController(PocAssignmentService pocAssignmentService) {
        this.pocAssignmentService = pocAssignmentService;
    }

    @Operation(summary = "List event POCs")
    @GetMapping("/{eventId}/pocs")
    public List<PocAssignmentDto> getAssignments(@PathVariable UUID eventId) {
        return pocAssignmentService.getAssignmentsForEvent(eventId);
    }

    @Operation(summary = "Assign POC", description = "Assigns a user as a POC for the event (409 if already assigned)")
    @PostMapping("/{eventId}/pocs")
    @ResponseStatus(HttpStatus.CREATED)
    public PocAssignmentDto assignPoc(@PathVariable UUID eventId, @Valid @RequestBody PocAssignRequest request) {
        return pocAssignmentService.assignPoc(eventId, request);
    }

    @Operation(summary = "Remove POC")
    @DeleteMapping("/{eventId}/pocs/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePoc(@PathVariable UUID eventId, @PathVariable UUID userId) {
        pocAssignmentService.removePoc(eventId, userId);
    }
}
