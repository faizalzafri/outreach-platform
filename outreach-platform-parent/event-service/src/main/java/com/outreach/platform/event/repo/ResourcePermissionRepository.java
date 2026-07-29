package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.ResourcePermission;
import com.outreach.platform.event.model.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ResourcePermission}.
 */
public interface ResourcePermissionRepository extends JpaRepository<ResourcePermission, UUID> {

    /**
     * Find all permissions for a specific resource.
     *
     * @param resourceType the type of resource
     * @param resourceId   the resource's UUID
     * @return list of permissions for the resource
     */
    List<ResourcePermission> findByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    /**
     * Find all permissions owned by a specific user.
     *
     * @param ownerUserId the owner's UUID
     * @return list of permissions owned by the user
     */
    List<ResourcePermission> findByOwnerUserId(UUID ownerUserId);

    /**
     * Find all permissions granted to a specific team.
     *
     * @param grantedTeamId the team's UUID
     * @return list of permissions granted to the team
     */
    List<ResourcePermission> findByGrantedTeamId(UUID grantedTeamId);

    /**
     * Delete all permissions granted to a specific team.
     * Used for cascade revocation when a team is deleted.
     *
     * @param grantedTeamId the team's UUID
     */
    void deleteByGrantedTeamId(UUID grantedTeamId);
}
