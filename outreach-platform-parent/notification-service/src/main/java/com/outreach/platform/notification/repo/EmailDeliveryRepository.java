package com.outreach.platform.notification.repo;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * MongoDB repository for email delivery tracking documents.
 *
 * <p>Request-facing reads must use the {@code ByTenantId...} variants (deliveries hold recipient
 * PII); the unscoped ones are for the cross-tenant PLATFORM_ADMIN view (empty TenantContext) and
 * the scheduled retry sweep.
 */
@Repository
public interface EmailDeliveryRepository extends MongoRepository<EmailDeliveryDocument, String> {

    List<EmailDeliveryDocument> findByEventId(String eventId);

    List<EmailDeliveryDocument> findByTenantIdAndEventId(UUID tenantId, String eventId);

    List<EmailDeliveryDocument> findByEventIdAndStatusIn(String eventId, Collection<DeliveryStatus> statuses);

    List<EmailDeliveryDocument> findByTenantIdAndEventIdAndStatusIn(
            UUID tenantId, String eventId, Collection<DeliveryStatus> statuses);

    Page<EmailDeliveryDocument> findByTenantId(UUID tenantId, Pageable pageable);

    List<EmailDeliveryDocument> findByStatusAndNextRetryAtBefore(DeliveryStatus status, Instant now);

    long countByTenantId(UUID tenantId);

    long countByStatus(DeliveryStatus status);

    long countByTenantIdAndStatus(UUID tenantId, DeliveryStatus status);
}
