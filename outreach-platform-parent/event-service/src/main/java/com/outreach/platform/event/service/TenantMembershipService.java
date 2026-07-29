package com.outreach.platform.event.service;

import com.outreach.platform.event.entity.TenantMembershipEntity;
import com.outreach.platform.event.model.TenantRole;
import com.outreach.platform.event.model.dto.MemberResponse;
import com.outreach.platform.event.repo.TeamMembershipRepository;
import com.outreach.platform.event.repo.TenantMembershipRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic service for tenant membership management.
 * Handles adding/removing members and enforces invariants such as
 * duplicate detection and the last-admin protection rule.
 */
@Service
public class TenantMembershipService {

    private static final Logger log = LoggerFactory.getLogger(TenantMembershipService.class);

    private final TenantMembershipRepository membershipRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    @Inject
    public TenantMembershipService(TenantMembershipRepository membershipRepository,
                                   TeamMembershipRepository teamMembershipRepository) {
        this.membershipRepository = membershipRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    /**
     * Adds a member to a tenant with the specified role.
     * <p>
     * If the role being assigned is PLATFORM_ADMIN, verifies that the currently
     * authenticated user already has ROLE_PLATFORM_ADMIN authority. Non-platform-admins
     * cannot grant the PLATFORM_ADMIN role (self-elevation prevention).
     *
     * @param tenantId the tenant UUID
     * @param userId   the user UUID to add
     * @param role     the role to assign
     * @return the created membership response
     * @throws DuplicateMembershipException      if the user already has this role in the tenant
     * @throws SelfElevationForbiddenException   if a non-platform-admin attempts to assign PLATFORM_ADMIN
     */
    @Transactional
    public MemberResponse addMember(UUID tenantId, UUID userId, TenantRole role) {
        if (role == TenantRole.PLATFORM_ADMIN) {
            enforceCallerIsPlatformAdmin();
        }

        if (membershipRepository.existsByTenantIdAndUserIdAndRole(tenantId, userId, role)) {
            throw new DuplicateMembershipException(tenantId, userId, role);
        }

        TenantMembershipEntity entity = new TenantMembershipEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setRole(role);

        TenantMembershipEntity saved = membershipRepository.save(entity);
        log.info("Added user {} to tenant {} with role {}", userId, tenantId, role);
        return toResponse(saved);
    }

    /**
     * Verifies the currently authenticated user holds ROLE_PLATFORM_ADMIN authority.
     * Rejects with {@link SelfElevationForbiddenException} if not.
     */
    private void enforceCallerIsPlatformAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new SelfElevationForbiddenException();
        }

        boolean isPlatformAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PLATFORM_ADMIN"::equals);

        if (!isPlatformAdmin) {
            log.warn("Self-elevation attempt: user '{}' tried to assign PLATFORM_ADMIN role without authority",
                    authentication.getName());
            throw new SelfElevationForbiddenException();
        }
    }

    /**
     * Removes a member from a tenant. Enforces the at-least-one-admin rule.
     *
     * @param tenantId the tenant UUID
     * @param userId   the user UUID to remove
     * @throws MemberNotFoundException if the user is not a member of the tenant
     * @throws LastAdminRemovalException if removing this user would leave no admins
     */
    @Transactional
    public void removeMember(UUID tenantId, UUID userId) {
        TenantMembershipEntity membership = membershipRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseThrow(() -> new MemberNotFoundException(tenantId, userId));

        if (membership.getRole() == TenantRole.ADMIN) {
            long adminCount = membershipRepository.countByTenantIdAndRole(tenantId, TenantRole.ADMIN);
            if (adminCount <= 1) {
                throw new LastAdminRemovalException(tenantId);
            }
        }

        membershipRepository.delete(membership);
        log.info("Removed user {} from tenant {}", userId, tenantId);

        teamMembershipRepository.deleteByUserIdAndTenantId(userId, tenantId);
        log.info("Cascade-deleted team memberships for user {} in tenant {}", userId, tenantId);
    }

    /**
     * Lists all members of a tenant with pagination.
     *
     * @param tenantId the tenant UUID
     * @param pageable pagination parameters
     * @return page of member responses
     */
    @Transactional(readOnly = true)
    public Page<MemberResponse> listMembers(UUID tenantId, Pageable pageable) {
        return membershipRepository.findByTenantId(tenantId, pageable)
                .map(this::toResponse);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private MemberResponse toResponse(TenantMembershipEntity entity) {
        return new MemberResponse(
                entity.getUserId(),
                entity.getRole(),
                entity.getCreatedDate()
        );
    }

    // ─── Exceptions ─────────────────────────────────────────────────────────────

    public static class DuplicateMembershipException extends RuntimeException {
        public DuplicateMembershipException(UUID tenantId, UUID userId, TenantRole role) {
            super("User " + userId + " already has role " + role + " in tenant " + tenantId);
        }
    }

    public static class LastAdminRemovalException extends RuntimeException {
        public LastAdminRemovalException(UUID tenantId) {
            super("Cannot remove last ADMIN from tenant " + tenantId);
        }
    }

    public static class MemberNotFoundException extends RuntimeException {
        public MemberNotFoundException(UUID tenantId, UUID userId) {
            super("User " + userId + " is not a member of tenant " + tenantId);
        }
    }

    public static class SelfElevationForbiddenException extends RuntimeException {
        public SelfElevationForbiddenException() {
            super("PLATFORM_ADMIN role can only be assigned by existing Platform_Admins");
        }
    }
}
