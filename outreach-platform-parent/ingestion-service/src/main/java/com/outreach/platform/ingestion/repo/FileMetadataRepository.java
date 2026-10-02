package com.outreach.platform.ingestion.repo;

import com.outreach.platform.ingestion.model.FileMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Spring Data MongoDB repository for file metadata documents.
 */
public interface FileMetadataRepository extends MongoRepository<FileMetadataDocument, String> {
}
