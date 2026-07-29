package com.outreach.platform.event.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.Team;
import com.outreach.platform.event.entity.TeamMembership;
import com.outreach.platform.event.model.dto.CreateTeamRequest;
import com.outreach.platform.event.model.dto.TeamMemberResponse;
import com.outreach.platform.event.model.dto.TeamResponse;
import com.outreach.platform.event.model.dto.UpdateTeamRequest;
import com.outreach.platform.event.repo.ResourcePermissionRepository;
import com.outreach.platform.event.repo.TeamMembershipRepository;
import com.outreach.platform.event.repo.TeamRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for team CRUD operations and team membership management.
 * All operations are scoped to the current tenant via {@link TenantContext}.
 */
@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final ResourcePermissionRepository resourcePermissionRepository;

    @Inject
    public TeamService(TeamRepository teamRepository, TeamMembershipRepository teamMembershipRepository,
                       ResourcePermissionRepository resourcePermissionRepository) {
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.resourcePermissionRepository = resourcePermissionRepository;
    }

    /**
     * Creates a new team within the current tenant.
     * Validates name uniqueness within the tenant.
     *
     * @param request the create request with name and description
     * @return the created team response
     * @throws DuplicateTeamNameException if a team with the same name exists in the tenant
     */
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        UUID tenantId = TenantContext.getCurrentTenantId();

        if (teamRepository.existsByTenantIdAndName(tenantId, request.name())) {
            throw new DuplicateTeamNameException(request.name());
        }

        Team team = new Team();
        team.setName(request.name());
        team.setDescription(request.description());

        Team saved = teamRepository.save(team);
        log.info("Created team '{}' (id={}) in tenant {}", saved.getName(), saved.getId(), tenantId);
        return toResponse(saved);
    }

    /**
     * Returns a paginated list of teams in the current tenant.
     * Tenant scoping is enforced automatically via Hibernate TenantFilter.
     *
     * @param pageable pagination parameters
     * @return page of team responses
     */
    @Transactional(readOnly = true)
    public Page<TeamResponse> listTeams(Pageable pageable) {
        return teamRepository.findAll(pageable).map(this::toResponse);
    }

    /**
     * Gets a team by ID within the current tenant.
     *
     * @param id the team UUID
     * @return the team response
     * @throws TeamNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public TeamResponse getTeam(UUID id) {
        Team team = findTeamOrThrow(id);
        return toResponse(team);
    }

    /**
     * Updates a team's name and description.
     * Validates name uniqueness within the tenant if the name is changed.
     *
     * @param id      the team UUID
     * @param request the update request with new name and description
     * @return the updated team response
     * @throws TeamNotFoundException      if not found
     * @throws DuplicateTeamNameException if new name already exists in the tenant
     */
    @Transactional
    public TeamResponse updateTeam(UUID id, UpdateTeamRequest request) {
        Team team = findTeamOrThrow(id);
        UUID tenantId = TenantContext.getCurrentTenantId();

        if (!team.getName().equals(request.name()) && teamRepository.existsByTenantIdAndName(tenantId, request.name())) {
            throw new DuplicateTeamNameException(request.name());
        }

        team.setName(request.name());
        team.setDescription(request.description());

        Team saved = teamRepository.save(team);
        log.info("Updated team {} name to '{}' in tenant {}", id, saved.getName(), tenantId);
        return toResponse(saved);
    }

    /**
     * Deletes a team and cascade-deletes all team memberships and resource permissions.
     *
     * @param id the team UUID
     * @throws TeamNotFoundException if not found
     */
    @Transactional
    public void deleteTeam(UUID id) {
        Team team = findTeamOrThrow(id);

        // Cascade revoke resource permissions granted to this team
        resourcePermissionRepository.deleteByGrantedTeamId(id);
        log.info("Cascade-revoked resource permissions for team {}", id);

        // Cascade delete team memberships
        teamMembershipRepository.deleteByTeamId(id);
        teamRepository.delete(team);

        log.info("Deleted team '{}' (id={}) and its memberships", team.getName(), id);
    }

    /**
     * Adds a user to a team.
     *
     * @param teamId the team UUID
     * @param userId the user UUID to add
     * @return the created membership response
     * @throws TeamNotFoundException           if the team is not found
     * @throws DuplicateTeamMemberException if the user is already a member
     */
    @Transactional
    public TeamMemberResponse addMember(UUID teamId, UUID userId) {
        findTeamOrThrow(teamId);

        if (teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new DuplicateTeamMemberException(teamId, userId);
        }

        TeamMembership membership = new TeamMembership();
        membership.setTeamId(teamId);
        membership.setUserId(userId);

        TeamMembership saved = teamMembershipRepository.save(membership);
        log.info("Added user {} to team {}", userId, teamId);
        return toMemberResponse(saved);
    }

    /**
     * Removes a user from a team.
     *
     * @param teamId the team UUID
     * @param userId the user UUID to remove
     * @throws TeamNotFoundException   if the team is not found
     * @throws MemberNotFoundException if the user is not a member of the team
     */
    @Transactional
    public void removeMember(UUID teamId, UUID userId) {
        findTeamOrThrow(teamId);

        if (!teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new MemberNotFoundException(teamId, userId);
        }

        teamMembershipRepository.deleteByTeamIdAndUserId(teamId, userId);
        log.info("Removed user {} from team {}", userId, teamId);
    }

    /**
     * Lists all members of a team (paginated).
     *
     * @param teamId   the team UUID
     * @param pageable pagination parameters
     * @return page of team member responses
     * @throws TeamNotFoundException if the team is not found
     */
    @Transactional(readOnly = true)
    public Page<TeamMemberResponse> listMembers(UUID teamId, Pageable pageable) {
        findTeamOrThrow(teamId);
        return teamMembershipRepository.findByTeamId(teamId, pageable).map(this::toMemberResponse);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Team findTeamOrThrow(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new TeamNotFoundException(id));
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCreatedDate()
        );
    }

    private TeamMemberResponse toMemberResponse(TeamMembership membership) {
        return new TeamMemberResponse(
                membership.getUserId(),
                membership.getCreatedDate()
        );
    }

    // ─── Exceptions ─────────────────────────────────────────────────────────────

    public static class TeamNotFoundException extends RuntimeException {
        public TeamNotFoundException(UUID id) {
            super("Team with ID " + id + " not found");
        }
    }

    public static class DuplicateTeamNameException extends RuntimeException {
        public DuplicateTeamNameException(String name) {
            super("Team with name '" + name + "' already exists in this tenant");
        }
    }

    public static class DuplicateTeamMemberException extends RuntimeException {
        public DuplicateTeamMemberException(UUID teamId, UUID userId) {
            super("User " + userId + " is already a member of team " + teamId);
        }
    }

    public static class MemberNotFoundException extends RuntimeException {
        public MemberNotFoundException(UUID teamId, UUID userId) {
            super("User " + userId + " is not a member of team " + teamId);
        }
    }
}
