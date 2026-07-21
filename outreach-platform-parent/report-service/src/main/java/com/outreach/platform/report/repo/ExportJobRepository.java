package com.outreach.platform.report.repo;

import com.outreach.platform.report.model.ExportJobDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for export job tracking documents.
 */
@Repository
public interface ExportJobRepository extends MongoRepository<ExportJobDocument, String> {

    Optional<ExportJobDocument> findByJobId(String jobId);
}
