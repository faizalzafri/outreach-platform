package com.outreach.platform.ai.repo;

import com.outreach.platform.ai.entity.AiJobDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for AI job documents.
 */
public interface AiJobRepository extends MongoRepository<AiJobDocument, String> {
}
