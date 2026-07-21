package com.outreach.platform.report.repo;

import com.outreach.platform.report.model.AnalyticsSnapshotDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data MongoDB repository for pre-computed analytics snapshots.
 */
@Repository
public interface AnalyticsSnapshotRepository extends MongoRepository<AnalyticsSnapshotDocument, String> {

    List<AnalyticsSnapshotDocument> findBySnapshotTypeAndPeriodStartBetween(
            String snapshotType, Instant start, Instant end);

    List<AnalyticsSnapshotDocument> findBySnapshotTypeOrderByPeriodStartDesc(String snapshotType);
}
