package com.outreach.platform.auth.repo;

import com.outreach.platform.auth.entity.TenantMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data JPA repository for {@link TenantMembership} entities. */
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, UUID> {

    List<TenantMembership> findByUserId(UUID userId);

    List<TenantMembership> findByTenantId(UUID tenantId);
}
