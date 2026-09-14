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

    /**
     * Tenant-scoped primary-key lookup — use instead of the inherited {@code findById}, which
     * compiles to {@code EntityManager.find()} and is not reached by Hibernate's {@code @Filter}.
     * See {@code docs/specs/platform-hardening/requirements.md} Finding 0 / Requirement 0.
     */
    Optional<NotificationTemplateEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<NotificationTemplateEntity> findByName(String name);

    List<NotificationTemplateEntity> findByTypeAndActive(NotificationType type, boolean active);

    List<NotificationTemplateEntity> findByActive(boolean active);
}
