package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.TeamMembership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TeamMembership}.
 */
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

    /**
     * Find all memberships for a given team.
     *
     * @param teamId the team's UUID
     * @return list of memberships in the team
     */
    List<TeamMembership> findByTeamId(UUID teamId);

    /**
     * Find all memberships for a given team (paginated).
     *
     * @param teamId   the team's UUID
     * @param pageable pagination parameters
     * @return page of memberships in the team
     */
    Page<TeamMembership> findByTeamId(UUID teamId, Pageable pageable);

    /**
     * Check if a user is already a member of a team.
     *
     * @param teamId the team's UUID
     * @param userId the user's UUID
     * @return true if the user is a member of the team
     */
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Count members of a team, for the team list page's member-count column.
     *
     * @param teamId the team's UUID
     * @return number of members
     */
    long countByTeamId(UUID teamId);

    /**
     * Delete all memberships for a given team (cascade on team deletion).
     *
     * @param teamId the team's UUID
     */
    void deleteByTeamId(UUID teamId);

    /**
     * Delete all team memberships for a user within a specific tenant.
     * Used for cascade removal when a user is removed from a tenant.
     *
     * @param userId   the user's UUID
     * @param tenantId the tenant's UUID
     */
    void deleteByUserIdAndTenantId(UUID userId, UUID tenantId);

    /**
     * Delete a specific user's membership in a team.
     *
     * @param teamId the team's UUID
     * @param userId the user's UUID
     */
    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);
}
