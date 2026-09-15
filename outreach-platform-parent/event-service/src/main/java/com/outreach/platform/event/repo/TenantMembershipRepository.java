package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.TenantMembershipEntity;
import com.outreach.platform.event.model.TenantRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TenantMembershipEntity}.
 */
public interface TenantMembershipRepository extends JpaRepository<TenantMembershipEntity, UUID> {

    /**
     * Find all memberships within a given tenant (paginated).
     *
     * @param tenantId the tenant's UUID
     * @param pageable pagination parameters
     * @return page of memberships in the tenant
     */
    Page<TenantMembershipEntity> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Check if a membership already exists for a user with a specific role in a tenant.
     *
     * @param tenantId the tenant's UUID
     * @param userId   the user's UUID
     * @param role     the tenant role
     * @return true if the membership exists
     */
    boolean existsByTenantIdAndUserIdAndRole(UUID tenantId, UUID userId, TenantRole role);

    /**
     * Find a membership by tenant, user, and role.
     *
     * @param tenantId the tenant's UUID
     * @param userId   the user's UUID
     * @param role     the tenant role
     * @return the membership if found
     */
    Optional<TenantMembershipEntity> findByTenantIdAndUserIdAndRole(UUID tenantId, UUID userId, TenantRole role);

    /**
     * Find a membership by tenant and user (regardless of role).
     *
     * @param tenantId the tenant's UUID
     * @param userId   the user's UUID
     * @return the membership if found
     */
    Optional<TenantMembershipEntity> findByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Check if a user has any membership in a given tenant.
     *
     * @param tenantId the tenant's UUID
     * @param userId   the user's UUID
     * @return true if user has a membership in the tenant
     */
    boolean existsByTenantIdAndUserId(UUID tenantId, UUID userId);

    /**
     * Count the number of memberships with a specific role in a tenant.
     *
     * @param tenantId the tenant's UUID
     * @param role     the role to count
     * @return the count of memberships with that role
     */
    long countByTenantIdAndRole(UUID tenantId, TenantRole role);

    /**
     * Check if a user has any membership in any tenant.
     *
     * @param userId the user's UUID
     * @return true if the user has at least one membership anywhere
     */
    boolean existsByUserId(UUID userId);
}
