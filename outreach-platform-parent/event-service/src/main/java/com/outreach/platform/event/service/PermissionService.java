package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.ResourcePermission;
import com.outreach.platform.event.model.PermissionLevel;
import com.outreach.platform.event.model.ResourceType;
import com.outreach.platform.event.model.Visibility;
import com.outreach.platform.event.repo.ResourcePermissionRepository;
import com.outreach.platform.event.repo.TeamMembershipRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Service enforcing resource access control based on visibility, ownership, team membership, and permission levels. */
@Service
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);

    private final ResourcePermissionRepository resourcePermissionRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    @Inject
    public PermissionService(ResourcePermissionRepository resourcePermissionRepository,
                             TeamMembershipRepository teamMembershipRepository) {
        this.resourcePermissionRepository = resourcePermissionRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    /**
     * Checks whether a user has at least the required permission level on a resource.
     *
     * @param userId       the user requesting access
     * @param resourceType the type of resource
     * @param resourceId   the resource identifier
     * @param required     the minimum permission level required
     * @return true if access is granted
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID userId, ResourceType resourceType, UUID resourceId, PermissionLevel required) {
        List<ResourcePermission> permissions = resourcePermissionRepository
                .findByResourceTypeAndResourceId(resourceType, resourceId);

        if (permissions.isEmpty()) {
            return false;
        }

        // Use the first permission record as the canonical resource metadata.
        // All records for the same resource share the same owner and visibility.
        ResourcePermission resource = permissions.get(0);

        // Step 1: Owner always has full access
        if (userId.equals(resource.getOwnerUserId())) {
            return true;
        }

        // Step 2: PRIVATE visibility — only owner has access (already checked above)
        if (resource.getVisibility() == Visibility.PRIVATE) {
            return false;
        }

        // Step 3: TENANT visibility — VIEW is granted to all, higher levels require explicit grant
        if (resource.getVisibility() == Visibility.TENANT) {
            if (required == PermissionLevel.VIEW) {
                return true;
            }
            // For EDIT or MANAGE, check if there is an explicit team-based grant
            return hasTeamGrantedAccess(userId, permissions, required);
        }

        // Step 4: TEAM visibility — check if user belongs to a granted team with sufficient level
        if (resource.getVisibility() == Visibility.TEAM) {
            return hasTeamGrantedAccess(userId, permissions, required);
        }

        return false;
    }

    /**
     * Shares a resource with a team by creating or updating a permission record.
     * Upgrades PRIVATE visibility to TEAM if needed.
     *
     * @param resourceId   the resource UUID
     * @param resourceType the type of resource
     * @param teamId       the team to share with
     * @param level        the permission level to grant
     */
    @Transactional
    public void shareWithTeam(UUID resourceId, ResourceType resourceType, UUID teamId, PermissionLevel level) {
        UUID currentUserId = getCurrentUserId();

        List<ResourcePermission> permissions = resourcePermissionRepository
                .findByResourceTypeAndResourceId(resourceType, resourceId);

        if (permissions.isEmpty()) {
            throw new ResourceNotFoundException(resourceType, resourceId);
        }

        ResourcePermission resource = permissions.get(0);

        // Verify current user is owner or has MANAGE permission
        if (!currentUserId.equals(resource.getOwnerUserId())
                && !hasAccess(currentUserId, resourceType, resourceId, PermissionLevel.MANAGE)) {
            throw new AccessDeniedException(resourceType, resourceId);
        }

        // Verify current user belongs to the target team (unless ADMIN)
        if (!isCurrentUserAdmin() && !teamMembershipRepository.existsByTeamIdAndUserId(teamId, currentUserId)) {
            throw new TeamMembershipRequiredException(teamId);
        }

        // Check if a permission already exists for this team on this resource
        ResourcePermission existingGrant = permissions.stream()
                .filter(p -> teamId.equals(p.getGrantedTeamId()))
                .findFirst()
                .orElse(null);

        if (existingGrant != null) {
            // Update existing grant
            existingGrant.setPermissionLevel(level);
            resourcePermissionRepository.save(existingGrant);
            log.info("Updated permission for resource {}:{} to team {} with level {}",
                    resourceType, resourceId, teamId, level);
        } else {
            // Create new permission record
            ResourcePermission newPermission = new ResourcePermission();
            newPermission.setResourceType(resourceType);
            newPermission.setResourceId(resourceId);
            newPermission.setVisibility(resource.getVisibility() == Visibility.PRIVATE
                    ? Visibility.TEAM : resource.getVisibility());
            newPermission.setOwnerUserId(resource.getOwnerUserId());
            newPermission.setGrantedTeamId(teamId);
            newPermission.setPermissionLevel(level);
            resourcePermissionRepository.save(newPermission);
            log.info("Created permission for resource {}:{} granting team {} level {}",
                    resourceType, resourceId, teamId, level);
        }

        // If visibility was PRIVATE, upgrade to TEAM
        if (resource.getVisibility() == Visibility.PRIVATE) {
            for (ResourcePermission p : permissions) {
                p.setVisibility(Visibility.TEAM);
                resourcePermissionRepository.save(p);
            }
            log.info("Upgraded visibility for resource {}:{} from PRIVATE to TEAM", resourceType, resourceId);
        }
    }

    /**
     * Changes the visibility of a resource. Requires owner or MANAGE permission.
     *
     * @param resourceId    the resource UUID
     * @param resourceType  the type of resource
     * @param newVisibility the new visibility level
     */
    @Transactional
    public void changeVisibility(UUID resourceId, ResourceType resourceType, Visibility newVisibility) {
        UUID currentUserId = getCurrentUserId();

        List<ResourcePermission> permissions = resourcePermissionRepository
                .findByResourceTypeAndResourceId(resourceType, resourceId);

        if (permissions.isEmpty()) {
            throw new ResourceNotFoundException(resourceType, resourceId);
        }

        ResourcePermission resource = permissions.get(0);

        // Verify current user is owner or has MANAGE permission
        if (!currentUserId.equals(resource.getOwnerUserId())
                && !hasAccess(currentUserId, resourceType, resourceId, PermissionLevel.MANAGE)) {
            throw new AccessDeniedException(resourceType, resourceId);
        }

        // If changing to TEAM, require at least one team to be granted access
        if (newVisibility == Visibility.TEAM) {
            boolean hasTeamGrant = permissions.stream()
                    .anyMatch(p -> p.getGrantedTeamId() != null);
            if (!hasTeamGrant) {
                throw new NoTeamGrantedException(resourceType, resourceId);
            }
        }

        // Update visibility on all permission records for this resource
        for (ResourcePermission p : permissions) {
            p.setVisibility(newVisibility);
            resourcePermissionRepository.save(p);
        }

        log.info("Changed visibility for resource {}:{} to {}", resourceType, resourceId, newVisibility);
    }

    /**
     * Lists all permission records for a resource. Requires at least VIEW access.
     *
     * @param resourceId   the resource UUID
     * @param resourceType the type of resource
     * @return list of permission records
     */
    @Transactional(readOnly = true)
    public List<ResourcePermission> listPermissions(UUID resourceId, ResourceType resourceType) {
        UUID currentUserId = getCurrentUserId();

        List<ResourcePermission> permissions = resourcePermissionRepository
                .findByResourceTypeAndResourceId(resourceType, resourceId);

        if (permissions.isEmpty()) {
            throw new ResourceNotFoundException(resourceType, resourceId);
        }

        ResourcePermission resource = permissions.get(0);

        // Verify current user is owner or has at least VIEW access
        if (!currentUserId.equals(resource.getOwnerUserId())
                && !hasAccess(currentUserId, resourceType, resourceId, PermissionLevel.VIEW)) {
            throw new AccessDeniedException(resourceType, resourceId);
        }

        return permissions;
    }

    /**
     * Revokes a specific permission by ID. Requires owner or MANAGE permission.
     *
     * @param permissionId the permission record UUID to revoke
     */
    @Transactional
    public void revokePermission(UUID permissionId) {
        UUID currentUserId = getCurrentUserId();

        ResourcePermission permission = resourcePermissionRepository.findById(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        // Verify current user is owner or has MANAGE permission on the resource
        if (!currentUserId.equals(permission.getOwnerUserId())
                && !hasAccess(currentUserId, permission.getResourceType(), permission.getResourceId(), PermissionLevel.MANAGE)) {
            throw new AccessDeniedException(permission.getResourceType(), permission.getResourceId());
        }

        resourcePermissionRepository.delete(permission);
        log.info("Revoked permission {} for resource {}:{}", permissionId,
                permission.getResourceType(), permission.getResourceId());
    }

    // ─── Internal Helpers ───────────────────────────────────────────────────────

    /** Checks if the user belongs to any team granted access at or above the required level. */
    private boolean hasTeamGrantedAccess(UUID userId, List<ResourcePermission> permissions, PermissionLevel required) {
        return permissions.stream()
                .filter(p -> p.getGrantedTeamId() != null)
                .filter(p -> meetsOrExceeds(p.getPermissionLevel(), required))
                .anyMatch(p -> teamMembershipRepository.existsByTeamIdAndUserId(p.getGrantedTeamId(), userId));
    }

    /** Returns true if granted level meets or exceeds required (VIEW < EDIT < MANAGE). */
    private boolean meetsOrExceeds(PermissionLevel granted, PermissionLevel required) {
        return granted.ordinal() >= required.ordinal();
    }

    /** Extracts the current user's UUID from the SecurityContext. */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("No authenticated user found in SecurityContext");
        }
        return UUID.fromString(authentication.getName());
    }

    /** Checks if the current user has ADMIN or PLATFORM_ADMIN role. */
    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth) || "ROLE_PLATFORM_ADMIN".equals(auth));
    }

    // ─── Exceptions ─────────────────────────────────────────────────────────────

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(ResourceType type, UUID id) {
            super("Resource " + type + " with ID " + id + " not found");
        }
    }

    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(ResourceType type, UUID id) {
            super("Access denied to resource " + type + " with ID " + id);
        }
    }

    public static class TeamMembershipRequiredException extends RuntimeException {
        public TeamMembershipRequiredException(UUID teamId) {
            super("Cannot share with team " + teamId + " — user is not a member of that team");
        }
    }

    public static class NoTeamGrantedException extends RuntimeException {
        public NoTeamGrantedException(ResourceType type, UUID id) {
            super("Cannot set visibility to TEAM for resource " + type + ":" + id
                    + " — at least one team must be granted access first");
        }
    }

    public static class PermissionNotFoundException extends RuntimeException {
        public PermissionNotFoundException(UUID permissionId) {
            super("Permission with ID " + permissionId + " not found");
        }
    }
}
