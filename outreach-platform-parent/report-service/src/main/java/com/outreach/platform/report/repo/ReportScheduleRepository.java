package com.outreach.platform.report.repo;

import com.outreach.platform.report.entity.ReportScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for scheduled report configurations.
 */
@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportScheduleEntity, UUID> {

    /**
     * Tenant-scoped primary-key lookup — use instead of the inherited {@code findById}, which
     * compiles to {@code EntityManager.find()} and is not reached by Hibernate's {@code @Filter}.
     * See CLAUDE.md.
     */
    Optional<ReportScheduleEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<ReportScheduleEntity> findAllByOrderByCreatedDateDesc();
}
