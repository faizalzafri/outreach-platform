package com.outreach.platform.notification.repo;

import com.outreach.platform.notification.entity.NotificationScheduleEntity;
import com.outreach.platform.notification.model.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for notification schedules.
 */
@Repository
public interface NotificationScheduleRepository extends JpaRepository<NotificationScheduleEntity, UUID> {

    List<NotificationScheduleEntity> findByTemplateId(UUID templateId);

    List<NotificationScheduleEntity> findByEventId(UUID eventId);

    List<NotificationScheduleEntity> findByStatus(ScheduleStatus status);
}
