package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.Tenant;
import com.outreach.platform.auth.entity.TenantMembership;
import com.outreach.platform.auth.model.TenantStatus;
import com.outreach.platform.auth.repo.TenantMembershipRepository;
import com.outreach.platform.auth.repo.TenantRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for resolving tenant memberships for authenticated users.
 *
 * <p>Used by the token customizer to determine which tenant a user belongs to
 * at authentication time, enabling the {@code tenant_id} claim to be embedded
 * in issued JWTs.</p>
 */
@Named
public class TenantMembershipService {

    private final TenantMembershipRepository tenantMembershipRepository;
    private final TenantRepository tenantRepository;

    @Inject
    public TenantMembershipService(TenantMembershipRepository tenantMembershipRepository,
                                   TenantRepository tenantRepository) {
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Finds all tenant memberships for a given user.
     *
     * @param userId the unique identifier of the user
     * @return list of memberships across all tenants the user belongs to
     */
    public List<TenantMembership> findMembershipsByUserId(UUID userId) {
        return tenantMembershipRepository.findByUserId(userId);
    }

    /**
     * Resolves the active tenant for a user at authentication time.
     *
     * <p>Iterates through the user's tenant memberships and returns the first
     * tenant that has an {@link TenantStatus#ACTIVE ACTIVE} status. This ensures
     * that users are not authenticated into suspended or deactivated tenants.</p>
     *
     * @param userId the unique identifier of the user
     * @return the first active tenant, or {@link Optional#empty()} if the user
     *         has no membership in any active tenant
     */
    public Optional<Tenant> getActiveTenantForUser(UUID userId) {
        List<TenantMembership> memberships = tenantMembershipRepository.findByUserId(userId);

        for (TenantMembership membership : memberships) {
            Optional<Tenant> tenant = tenantRepository.findById(membership.getTenantId());
            if (tenant.isPresent() && tenant.get().getStatus() == TenantStatus.ACTIVE) {
                return tenant;
            }
        }

        return Optional.empty();
    }
}
