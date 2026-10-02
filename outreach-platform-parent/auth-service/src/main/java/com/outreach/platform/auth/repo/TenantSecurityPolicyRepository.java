package com.outreach.platform.auth.repo;

import com.outreach.platform.auth.entity.TenantSecurityPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantSecurityPolicyRepository extends JpaRepository<TenantSecurityPolicy, UUID> {

    Optional<TenantSecurityPolicy> findByTenantId(UUID tenantId);

    List<TenantSecurityPolicy> findByTenantIdIn(Collection<UUID> tenantIds);
}
