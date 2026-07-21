package com.outreach.platform.ingestion.repo;

import com.outreach.platform.ingestion.model.JobTrackingDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for job tracking documents.
 */
public interface JobTrackingRepository extends MongoRepository<JobTrackingDocument, String> {

    Page<JobTrackingDocument> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
