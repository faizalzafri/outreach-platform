package com.outreach.platform.event.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.Team;
import com.outreach.platform.event.entity.TeamMembership;
import com.outreach.platform.event.entity.UserEntity;
import com.outreach.platform.event.model.dto.AvailableUserResponse;
import com.outreach.platform.event.model.dto.CreateTeamRequest;
import com.outreach.platform.event.model.dto.TeamMemberResponse;
import com.outreach.platform.event.model.dto.TeamResponse;
import com.outreach.platform.event.model.dto.UpdateTeamRequest;
import com.outreach.platform.event.repo.ResourcePermissionRepository;
import com.outreach.platform.event.repo.TeamMembershipRepository;
import com.outreach.platform.event.repo.TeamRepository;
import com.outreach.platform.event.repo.UserRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service for team CRUD operations and team membership management.
 * All operations are scoped to the current tenant via {@link TenantContext}.
 */
@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private static final int AVAILABLE_USERS_MAX_RESULTS = 20;

    private final TeamRepository teamRepository;
    private final TeamMembershipRepository teamMembershipRepository;
    private final ResourcePermissionRepository resourcePermissionRepository;
    private final UserRepository userRepository;

    @Inject
    public TeamService(TeamRepository teamRepository, TeamMembershipRepository teamMembershipRepository,
                       ResourcePermissionRepository resourcePermissionRepository, UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMembershipRepository = teamMembershipRepository;
        this.resourcePermissionRepository = resourcePermissionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new team within the current tenant. Validates name uniqueness.
     *
     * @param request the create request with name and description
     * @return the created team response
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
        return toResponse(saved, 0);
    }

    /**
     * Returns a paginated list of teams in the current tenant.
     *
     * @param pageable pagination parameters
     * @return page of team responses
     */
    @Transactional(readOnly = true)
    public Page<TeamResponse> listTeams(Pageable pageable) {
        return teamRepository.findAll(pageable)
                .map(team -> toResponse(team, teamMembershipRepository.countByTeamId(team.getId())));
    }

    /**
     * Gets a team by ID.
     *
     * @param id the team UUID
     * @return the team response
     */
    @Transactional(readOnly = true)
    public TeamResponse getTeam(UUID id) {
        Team team = findTeamOrThrow(id);
        return toResponse(team, teamMembershipRepository.countByTeamId(id));
    }

    /**
     * Updates a team's name and description. Validates name uniqueness if changed.
     *
     * @param id      the team UUID
     * @param request the update request
     * @return the updated team response
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
        return toResponse(saved, teamMembershipRepository.countByTeamId(id));
    }

    /**
     * Deletes a team and cascade-deletes memberships and resource permissions.
     *
     * @param id the team UUID
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
     */
    @Transactional
    public TeamMemberResponse addMember(UUID teamId, UUID userId) {
        findTeamOrThrow(teamId);

        if (teamMembershipRepository.existsByTeamIdAndUserId(teamId, userId)) {
            throw new DuplicateTeamMemberException(teamId, userId);
        }

        UserEntity user = userRepository.findByIdAndTenantId(userId, TenantContext.getCurrentTenantId())
                .orElseThrow(() -> new UserNotFoundException(userId));

        TeamMembership membership = new TeamMembership();
        membership.setTeamId(teamId);
        membership.setUserId(userId);

        TeamMembership saved = teamMembershipRepository.save(membership);
        log.info("Added user {} to team {}", userId, teamId);
        return toMemberResponse(saved, user);
    }

    /**
     * Removes a user from a team.
     *
     * @param teamId the team UUID
     * @param userId the user UUID to remove
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
     * Lists all members of a team (paginated), enriched with each member's username/email.
     *
     * @param teamId   the team UUID
     * @param pageable pagination parameters
     * @return page of team member responses
     */
    @Transactional(readOnly = true)
    public Page<TeamMemberResponse> listMembers(UUID teamId, Pageable pageable) {
        findTeamOrThrow(teamId);
        Page<TeamMembership> memberships = teamMembershipRepository.findByTeamId(teamId, pageable);

        List<UUID> userIds = memberships.map(TeamMembership::getUserId).toList();
        Map<UUID, UserEntity> usersById = userRepository.findByIdIn(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(UserEntity::getId, Function.identity()));

        return memberships.map(membership -> toMemberResponse(membership, usersById.get(membership.getUserId())));
    }

    /**
     * Searches users in the current tenant not already on the given team, for the "add member"
     * selector's search-as-you-type field. Deliberately unpaged and capped at a small fixed limit
     * — this is a typeahead, not a browsable list.
     *
     * @param teamId the team UUID (used only to exclude its current members)
     * @param search search term matched case-insensitively against username
     * @return up to {@value #AVAILABLE_USERS_MAX_RESULTS} matching users
     */
    @Transactional(readOnly = true)
    public List<AvailableUserResponse> searchAvailableUsers(UUID teamId, String search) {
        findTeamOrThrow(teamId);
        String term = StringUtils.hasText(search) ? search : "";
        Pageable limit = Pageable.ofSize(AVAILABLE_USERS_MAX_RESULTS);

        List<UUID> existingMemberIds = teamMembershipRepository.findByTeamId(teamId).stream()
                .map(TeamMembership::getUserId)
                .toList();

        Page<UserEntity> matches = existingMemberIds.isEmpty()
                ? userRepository.findByUsernameContainingIgnoreCase(term, limit)
                : userRepository.findByUsernameContainingIgnoreCaseAndIdNotIn(term, existingMemberIds, limit);

        return matches.map(this::toAvailableUserResponse).toList();
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Team findTeamOrThrow(UUID id) {
        // findById() alone does not enforce tenant isolation on this codebase's Hibernate version —
        // see docs/specs/platform-hardening/ Finding 0 / Requirement 0.
        Optional<Team> team = TenantContext.isPresent()
                ? teamRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                : teamRepository.findById(id);
        return team.orElseThrow(() -> new TeamNotFoundException(id));
    }

    private TeamResponse toResponse(Team team, long memberCount) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                memberCount,
                team.getCreatedDate()
        );
    }

    /** {@code user} may be null if it was deleted after joining the team — fall back gracefully rather than 500. */
    private TeamMemberResponse toMemberResponse(TeamMembership membership, UserEntity user) {
        return new TeamMemberResponse(
                membership.getUserId(),
                user != null ? user.getUsername() : null,
                user != null ? user.getEmail() : null,
                membership.getCreatedDate()
        );
    }

    private AvailableUserResponse toAvailableUserResponse(UserEntity user) {
        return new AvailableUserResponse(user.getId(), user.getUsername(), user.getEmail());
    }

    // ─── Exceptions ─────────────────────────────────────────────────────────────

    public static class TeamNotFoundException extends RuntimeException {
        public TeamNotFoundException(UUID id) {
            super("Team with ID " + id + " not found");
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(UUID id) {
            super("User with ID " + id + " not found");
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
