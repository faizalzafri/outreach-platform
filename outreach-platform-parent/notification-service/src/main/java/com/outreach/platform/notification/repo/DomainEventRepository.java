package com.outreach.platform.notification.repo;

import com.outreach.platform.notification.model.DomainEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for domain event documents consumed by the notification service.
 */
@Repository
public interface DomainEventRepository extends MongoRepository<DomainEventDocument, String> {

    List<DomainEventDocument> findByEventTypeInAndProcessedFalse(List<String> eventTypes);
}
