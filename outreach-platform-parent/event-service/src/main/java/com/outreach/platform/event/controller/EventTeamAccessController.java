package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.TeamAccessDto;
import com.outreach.platform.event.model.dto.TeamAccessRequest;
import com.outreach.platform.event.service.EventTeamAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The teams an event is shared with. */
@RestController
@RequestMapping("/events/{eventId}/teams")
@Tag(name = "Event Sharing", description = "Share an event with teams so their POCs can work on it")
@PreAuthorize("hasAnyRole('PMO', 'ADMIN', 'TENANT_ADMIN', 'PLATFORM_ADMIN')")
public class EventTeamAccessController {

    private final EventTeamAccessService accessService;

    @Inject
    public EventTeamAccessController(EventTeamAccessService accessService) {
        this.accessService = accessService;
    }

    @Operation(summary = "List the teams an event is shared with")
    @GetMapping
    public List<TeamAccessDto> list(@PathVariable UUID eventId) {
        return accessService.list(eventId);
    }

    @Operation(summary = "Share with a team", description = "Gives the team access, or changes it; returns the event's teams")
    @PutMapping("/{teamId}")
    public List<TeamAccessDto> grant(@PathVariable UUID eventId, @PathVariable UUID teamId,
                                     @Valid @RequestBody TeamAccessRequest request) {
        return accessService.grant(eventId, teamId, request.accessLevel());
    }

    @Operation(summary = "Stop sharing with a team")
    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID eventId, @PathVariable UUID teamId) {
        accessService.revoke(eventId, teamId);
    }
}
