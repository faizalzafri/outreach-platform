package com.outreach.platform.ingestion.repo;

import com.outreach.platform.ingestion.model.DomainEventDocument;
import com.outreach.platform.ingestion.model.EventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Spring Data MongoDB repository for domain event outbox documents.
 */
public interface DomainEventRepository extends MongoRepository<DomainEventDocument, String> {

    List<DomainEventDocument> findByStatus(EventStatus status);
}
