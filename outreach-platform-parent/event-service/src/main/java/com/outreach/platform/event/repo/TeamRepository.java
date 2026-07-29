package com.outreach.platform.event.repo;

import com.outreach.platform.event.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Team}.
 */
public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * Find all teams within a given tenant (paginated).
     *
     * @param tenantId the tenant's UUID
     * @param pageable pagination parameters
     * @return page of teams in the tenant
     */
    Page<Team> findByTenantId(UUID tenantId, Pageable pageable);

    /**
     * Find a team by tenant and name.
     *
     * @param tenantId the tenant's UUID
     * @param name     the team name
     * @return the team if found
     */
    Optional<Team> findByTenantIdAndName(UUID tenantId, String name);

    /**
     * Check if a team with the given name exists within a tenant.
     *
     * @param tenantId the tenant's UUID
     * @param name     the team name
     * @return true if a team with this name exists in the tenant
     */
    boolean existsByTenantIdAndName(UUID tenantId, String name);
}
