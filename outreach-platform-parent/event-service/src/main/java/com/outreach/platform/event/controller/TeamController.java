package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.AddTeamMemberRequest;
import com.outreach.platform.event.model.dto.CreateTeamRequest;
import com.outreach.platform.event.model.dto.TeamMemberResponse;
import com.outreach.platform.event.model.dto.TeamResponse;
import com.outreach.platform.event.model.dto.UpdateTeamRequest;
import com.outreach.platform.event.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for team CRUD operations and team membership management.
 * All endpoints require TENANT_ADMIN, ADMIN, or PLATFORM_ADMIN role.
 * Tenant scoping is enforced automatically via the Hibernate TenantFilter.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/teams")
@PreAuthorize("hasAnyRole('TENANT_ADMIN', 'ADMIN', 'PLATFORM_ADMIN')")
@Tag(name = "Team Management", description = "Create, read, update, and delete teams; manage team membership")
public class TeamController {

    private final TeamService teamService;

    @Inject
    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @Operation(summary = "Create team", description = "Creates a new team within the current tenant")
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse created = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "List teams", description = "Returns a paginated list of teams in the current tenant")
    @GetMapping
    public ResponseEntity<Page<TeamResponse>> listTeams(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20)")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<TeamResponse> teams = teamService.listTeams(pageable);
        return ResponseEntity.ok(teams);
    }

    @Operation(summary = "Get team", description = "Returns a single team by ID")
    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeam(
            @Parameter(description = "Team UUID") @PathVariable UUID id) {
        TeamResponse team = teamService.getTeam(id);
        return ResponseEntity.ok(team);
    }

    @Operation(summary = "Update team", description = "Updates an existing team's name and description")
    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(
            @Parameter(description = "Team UUID") @PathVariable UUID id,
            @Valid @RequestBody UpdateTeamRequest request) {
        TeamResponse updated = teamService.updateTeam(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete team", description = "Deletes a team and cascade-deletes all team memberships")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @Parameter(description = "Team UUID") @PathVariable UUID id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add team member", description = "Adds a user to the specified team")
    @PostMapping("/{id}/members")
    public ResponseEntity<TeamMemberResponse> addMember(
            @Parameter(description = "Team UUID") @PathVariable UUID id,
            @Valid @RequestBody AddTeamMemberRequest request) {
        TeamMemberResponse response = teamService.addMember(id, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Remove team member", description = "Removes a user from the specified team")
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @Parameter(description = "Team UUID") @PathVariable UUID id,
            @Parameter(description = "User UUID to remove") @PathVariable UUID userId) {
        teamService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List team members", description = "Returns a paginated list of team members")
    @GetMapping("/{id}/members")
    public ResponseEntity<Page<TeamMemberResponse>> listMembers(
            @Parameter(description = "Team UUID") @PathVariable UUID id,
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (default 20)")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdDate"));
        Page<TeamMemberResponse> members = teamService.listMembers(id, pageable);
        return ResponseEntity.ok(members);
    }
}
