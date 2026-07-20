package com.outreach.platform.notification.repo;

import com.outreach.platform.notification.entity.NotificationTemplateEntity;
import com.outreach.platform.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for notification templates.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, UUID> {

    Optional<NotificationTemplateEntity> findByName(String name);

    List<NotificationTemplateEntity> findByTypeAndActive(NotificationType type, boolean active);

    List<NotificationTemplateEntity> findByActive(boolean active);
}
