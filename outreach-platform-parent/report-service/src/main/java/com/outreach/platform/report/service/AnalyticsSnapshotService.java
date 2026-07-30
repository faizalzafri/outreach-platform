package com.outreach.platform.report.service;

import com.outreach.platform.report.config.ReportServiceProperties;
import com.outreach.platform.report.model.AnalyticsSnapshotDocument;
import com.outreach.platform.report.repo.AnalyticsSnapshotRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Service for managing pre-computed analytics snapshots stored in MongoDB.
 * Snapshots are time-bucketed aggregation results used for fast dashboard rendering.
 */
@Service
public class AnalyticsSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsSnapshotService.class);

    private final AnalyticsSnapshotRepository snapshotRepository;
    private final ReportServiceProperties properties;

    @Inject
    public AnalyticsSnapshotService(AnalyticsSnapshotRepository snapshotRepository,
                                    ReportServiceProperties properties) {
        this.snapshotRepository = snapshotRepository;
        this.properties = properties;
    }

/**
     * Stores a new analytics snapshot with automatic TTL based on retention configuration.
     */
    public AnalyticsSnapshotDocument saveSnapshot(String snapshotType,
                                                   String granularity,
                                                   Instant periodStart,
                                                   Instant periodEnd,
                                                   Map<String, String> dimensions,
                                                   Map<String, Object> metrics) {
        AnalyticsSnapshotDocument snapshot = new AnalyticsSnapshotDocument();
        snapshot.setSnapshotType(snapshotType);
        snapshot.setGranularity(granularity);
        snapshot.setPeriodStart(periodStart);
        snapshot.setPeriodEnd(periodEnd);
        snapshot.setDimensions(dimensions);
        snapshot.setMetrics(metrics);
        snapshot.setGeneratedAt(Instant.now());
        snapshot.setExpiresAt(Instant.now().plus(properties.snapshotRetentionDays(), ChronoUnit.DAYS));
        snapshot.setGeneratedBy("report-service");

        AnalyticsSnapshotDocument saved = snapshotRepository.save(snapshot);
        log.debug("Saved analytics snapshot: type={}, period={} to {}", snapshotType, periodStart, periodEnd);
        return saved;
    }

    public List<AnalyticsSnapshotDocument> getSnapshots(String snapshotType, Instant start, Instant end) {
        return snapshotRepository.findBySnapshotTypeAndPeriodStartBetween(snapshotType, start, end);
    }

    public List<AnalyticsSnapshotDocument> getLatestSnapshots(String snapshotType) {
        return snapshotRepository.findBySnapshotTypeOrderByPeriodStartDesc(snapshotType);
    }
}
