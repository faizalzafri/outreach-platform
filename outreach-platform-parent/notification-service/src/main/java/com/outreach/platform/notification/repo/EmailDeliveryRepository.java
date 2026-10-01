package com.outreach.platform.notification.repo;

import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB repository for email delivery tracking documents.
 */
@Repository
public interface EmailDeliveryRepository extends MongoRepository<EmailDeliveryDocument, String> {

    List<EmailDeliveryDocument> findByEventId(String eventId);

    List<EmailDeliveryDocument> findByEventIdAndStatus(String eventId, DeliveryStatus status);

    List<EmailDeliveryDocument> findByStatusAndNextRetryAtBefore(DeliveryStatus status, Instant now);

    long countByStatus(DeliveryStatus status);
}
