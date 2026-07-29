package com.outreach.platform.auth.repo;

import com.outreach.platform.auth.entity.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TenantMembership} entities.
 */
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    /**
     * Find all tenant memberships for a given user.
     *
     * @param userId the user's UUID
     * @return list of memberships across all tenants
     */
    List<TenantMembership> findByUserId(UUID userId);

    /**
     * Find all memberships within a given tenant.
     *
     * @param tenantId the tenant's UUID
     * @return list of memberships in the tenant
     */
    List<TenantMembership> findByTenantId(UUID tenantId);
}
