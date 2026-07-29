package com.outreach.platform.auth.repo;

import com.outreach.platform.auth.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Tenant} entities.
 */
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    /**
     * Find a tenant by its unique slug.
     *
     * @param slug the URL-friendly tenant identifier
     * @return the tenant if found
     */
    Optional<Tenant> findBySlug(String slug);
}
