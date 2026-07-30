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

/** Resolves tenant memberships for authenticated users at token-issuance time. */
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


    public List<TenantMembership> findMembershipsByUserId(UUID userId) {
        return tenantMembershipRepository.findByUserId(userId);
    }

    /** Returns the first active tenant the user belongs to, or empty if none. */
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
