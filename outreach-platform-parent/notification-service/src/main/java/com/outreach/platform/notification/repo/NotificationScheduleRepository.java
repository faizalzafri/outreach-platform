package com.outreach.platform.notification.repo;

import com.outreach.platform.notification.entity.NotificationScheduleEntity;
import com.outreach.platform.notification.model.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for notification schedules.
 */
@Repository
public interface NotificationScheduleRepository extends JpaRepository<NotificationScheduleEntity, UUID> {

    /**
     * Tenant-scoped primary-key lookup — use instead of the inherited {@code findById}, which
     * compiles to {@code EntityManager.find()} and is not reached by Hibernate's {@code @Filter}.
     * See {@code docs/specs/platform-hardening/requirements.md} Finding 0 / Requirement 0.
     */
    Optional<NotificationScheduleEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    List<NotificationScheduleEntity> findByTemplateId(UUID templateId);

    List<NotificationScheduleEntity> findByEventId(UUID eventId);

    List<NotificationScheduleEntity> findByStatus(ScheduleStatus status);
}
