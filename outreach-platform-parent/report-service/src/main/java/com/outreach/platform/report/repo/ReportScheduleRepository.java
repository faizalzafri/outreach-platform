package com.outreach.platform.report.repo;

import com.outreach.platform.report.entity.ReportScheduleEntity;
import com.outreach.platform.report.model.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for scheduled report configurations.
 */
@Repository
public interface ReportScheduleRepository extends JpaRepository<ReportScheduleEntity, UUID> {

    List<ReportScheduleEntity> findByStatus(ScheduleStatus status);

    List<ReportScheduleEntity> findAllByOrderByCreatedAtDesc();
}
