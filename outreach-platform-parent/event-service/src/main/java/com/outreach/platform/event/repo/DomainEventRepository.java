package com.outreach.platform.event.repo;

import com.outreach.platform.event.model.DomainEventDocument;
import com.outreach.platform.event.model.DomainEventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data MongoDB repository for domain event outbox documents.
 */
@Repository
public interface DomainEventRepository extends MongoRepository<DomainEventDocument, String> {

    List<DomainEventDocument> findByStatus(DomainEventStatus status);

    List<DomainEventDocument> findByStatusOrderByCreatedAtAsc(DomainEventStatus status);
}
