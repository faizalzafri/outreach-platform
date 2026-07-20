package com.outreach.platform.event.config;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * MongoDB collection and index initializer for the Outreach Platform.
 *
 * <p>Creates all required collections and indexes on application startup via
 * {@link ApplicationReadyEvent}. Collections are created if they do not already
 * exist; indexes are ensured (created if missing, existing ones left untouched).</p>
 *
 * <p>This component is designed to be reusable — other services (notification-service,
 * ingestion-service, report-service, ai-service) can adapt it by including the relevant
 * collection definitions for their domain.</p>
 *
 * <h2>Collections managed:</h2>
 * <ul>
 *   <li>{@code domain_events} — outbox pattern for inter-service events (TTL: 30 days)</li>
 *   <li>{@code audit_logs} — audit trail for mutations (TTL: 90 days)</li>
 *   <li>{@code job_tracking} — Excel import jobs, email dispatch batches (TTL: 60 days)</li>
 *   <li>{@code email_deliveries} — email delivery status tracking (TTL: 90 days)</li>
 *   <li>{@code analytics_snapshots} — pre-computed dashboard data (TTL: 365 days)</li>
 *   <li>{@code file_metadata} — file processing state (TTL: 90 days)</li>
 * </ul>
 *
 * <p>All timestamps in MongoDB documents use ISODate format.</p>
 */
@Component
public class MongoDbInitializer {

    private static final Logger log = LoggerFactory.getLogger(MongoDbInitializer.class);

    // TTL durations for automatic document expiration
    private static final Duration TTL_30_DAYS = Duration.ofDays(30);
    private static final Duration TTL_60_DAYS = Duration.ofDays(60);
    private static final Duration TTL_90_DAYS = Duration.ofDays(90);
    private static final Duration TTL_365_DAYS = Duration.ofDays(365);

    // Collection name constants
    public static final String COLLECTION_DOMAIN_EVENTS = "domain_events";
    public static final String COLLECTION_AUDIT_LOGS = "audit_logs";
    public static final String COLLECTION_JOB_TRACKING = "job_tracking";
    public static final String COLLECTION_EMAIL_DELIVERIES = "email_deliveries";
    public static final String COLLECTION_ANALYTICS_SNAPSHOTS = "analytics_snapshots";
    public static final String COLLECTION_FILE_METADATA = "file_metadata";

    /**
     * All collections managed by this initializer.
     */
    public static final List<String> REQUIRED_COLLECTIONS = List.of(
            COLLECTION_DOMAIN_EVENTS,
            COLLECTION_AUDIT_LOGS,
            COLLECTION_JOB_TRACKING,
            COLLECTION_EMAIL_DELIVERIES,
            COLLECTION_ANALYTICS_SNAPSHOTS,
            COLLECTION_FILE_METADATA
    );

    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;

    @Inject
    public MongoDbInitializer(ObjectProvider<MongoTemplate> mongoTemplateProvider) {
        this.mongoTemplateProvider = mongoTemplateProvider;
    }

    /**
     * Initializes MongoDB collections and indexes after the application is ready.
     * Collections are created if they do not already exist. Indexes are ensured
     * (created if missing, existing ones left untouched).
     * If MongoTemplate is not available (MongoDB auto-config excluded), this is a no-op.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeCollectionsAndIndexes() {
        MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
        if (mongoTemplate == null) {
            log.info("MongoDB not configured — skipping collection/index initialization");
            return;
        }
        log.info("MongoDB initialization starting — ensuring collections and indexes");

        createCollections(mongoTemplate);
        createDomainEventsIndexes(mongoTemplate);
        createAuditLogsIndexes(mongoTemplate);
        createJobTrackingIndexes(mongoTemplate);
        createEmailDeliveriesIndexes(mongoTemplate);
        createAnalyticsSnapshotsIndexes(mongoTemplate);
        createFileMetadataIndexes(mongoTemplate);

        log.info("MongoDB initialization complete — {} collections configured", REQUIRED_COLLECTIONS.size());
    }

    private void createCollections(MongoTemplate mongoTemplate) {
        Set<String> existing = mongoTemplate.getCollectionNames().stream()
                .collect(Collectors.toSet());

        for (String collection : REQUIRED_COLLECTIONS) {
            if (!existing.contains(collection)) {
                mongoTemplate.createCollection(collection);
                log.info("Created MongoDB collection: {}", collection);
            } else {
                log.debug("MongoDB collection already exists: {}", collection);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // domain_events — outbox pattern for inter-service events
    // ─────────────────────────────────────────────────────────────────────────

    private void createDomainEventsIndexes(MongoTemplate mongoTemplate) {
        IndexOperations ops = mongoTemplate.indexOps(COLLECTION_DOMAIN_EVENTS);

        // Compound index for outbox poller: find PENDING events ordered by creation time
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.ASC)
                .named("idx_domain_events_status_createdAt"));

        // Index for querying by event type
        ops.ensureIndex(new Index()
                .on("eventType", Sort.Direction.ASC)
                .named("idx_domain_events_eventType"));

        // TTL index: auto-delete events after 30 days
        ops.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.ASC)
                .expire(TTL_30_DAYS)
                .named("idx_domain_events_ttl_30d"));

        log.debug("Indexes ensured for collection: {}", COLLECTION_DOMAIN_EVENTS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // audit_logs — audit trail for mutations
    // ─────────────────────────────────────────────────────────────────────────

    private void createAuditLogsIndexes(MongoTemplate mongoTemplate) {
        IndexOperations ops = mongoTemplate.indexOps(COLLECTION_AUDIT_LOGS);

        // Compound index for user activity queries (user + most recent first)
        ops.ensureIndex(new Index()
                .on("userId", Sort.Direction.ASC)
                .on("timestamp", Sort.Direction.DESC)
                .named("idx_audit_logs_userId_timestamp"));

        // Index for querying by action type
        ops.ensureIndex(new Index()
                .on("action", Sort.Direction.ASC)
                .named("idx_audit_logs_action"));

        // Compound index for resource-specific audit trail
        ops.ensureIndex(new Index()
                .on("resourceType", Sort.Direction.ASC)
                .on("resourceId", Sort.Direction.ASC)
                .named("idx_audit_logs_resource"));

        // TTL index: auto-delete audit logs after 90 days
        ops.ensureIndex(new Index()
                .on("timestamp", Sort.Direction.ASC)
                .expire(TTL_90_DAYS)
                .named("idx_audit_logs_ttl_90d"));

        log.debug("Indexes ensured for collection: {}", COLLECTION_AUDIT_LOGS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // job_tracking — Excel import jobs, email dispatch batches, report exports
    // ─────────────────────────────────────────────────────────────────────────

    private void createJobTrackingIndexes(MongoTemplate mongoTemplate) {
        IndexOperations ops = mongoTemplate.indexOps(COLLECTION_JOB_TRACKING);

        // Compound index for status-based queries (active jobs, most recent first)
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_job_tracking_status_createdAt"));

        // Index for filtering by job type
        ops.ensureIndex(new Index()
                .on("jobType", Sort.Direction.ASC)
                .named("idx_job_tracking_jobType"));

        // TTL index: auto-delete completed jobs after 60 days
        ops.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.ASC)
                .expire(TTL_60_DAYS)
                .named("idx_job_tracking_ttl_60d"));

        log.debug("Indexes ensured for collection: {}", COLLECTION_JOB_TRACKING);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // email_deliveries — email delivery status tracking
    // ─────────────────────────────────────────────────────────────────────────

    private void createEmailDeliveriesIndexes(MongoTemplate mongoTemplate) {
        IndexOperations ops = mongoTemplate.indexOps(COLLECTION_EMAIL_DELIVERIES);

        // Compound index for event-specific delivery status queries
        ops.ensureIndex(new Index()
                .on("eventId", Sort.Direction.ASC)
                .on("status", Sort.Direction.ASC)
                .named("idx_email_deliveries_eventId_status"));

        // Index for recipient lookups
        ops.ensureIndex(new Index()
                .on("recipientEmail", Sort.Direction.ASC)
                .named("idx_email_deliveries_recipientEmail"));

        // Compound index for retry poller (find failed deliveries to retry)
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .on("lastAttemptAt", Sort.Direction.ASC)
                .named("idx_email_deliveries_status_lastAttemptAt"));

        // TTL index: auto-delete delivery records after 90 days
        ops.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.ASC)
                .expire(TTL_90_DAYS)
                .named("idx_email_deliveries_ttl_90d"));

        log.debug("Indexes ensured for collection: {}", COLLECTION_EMAIL_DELIVERIES);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // analytics_snapshots — pre-computed dashboard data
    // ─────────────────────────────────────────────────────────────────────────

    private void createAnalyticsSnapshotsIndexes(MongoTemplate mongoTemplate) {
        IndexOperations ops = mongoTemplate.indexOps(COLLECTION_ANALYTICS_SNAPSHOTS);

        // Compound index for snapshot type queries (most recent first)
        ops.ensureIndex(new Index()
                .on("snapshotType", Sort.Direction.ASC)
                .on("createdAt", Sort.Direction.DESC)
                .named("idx_analytics_snapshots_type_createdAt"));

        // Index for event-specific snapshots
        ops.ensureIndex(new Index()
                .on("eventId", Sort.Direction.ASC)
                .named("idx_analytics_snapshots_eventId"));

        // TTL index: auto-delete snapshots after 365 days
        ops.ensureIndex(new Index()
                .on("createdAt", Sort.Direction.ASC)
                .expire(TTL_365_DAYS)
                .named("idx_analytics_snapshots_ttl_365d"));

        log.debug("Indexes ensured for collection: {}", COLLECTION_ANALYTICS_SNAPSHOTS);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // file_metadata — file processing state
    // ─────────────────────────────────────────────────────────────────────────

    private void createFileMetadataIndexes(MongoTemplate mongoTemplate) {
        IndexOperations ops = mongoTemplate.indexOps(COLLECTION_FILE_METADATA);

        // Index for job-specific file lookups
        ops.ensureIndex(new Index()
                .on("jobId", Sort.Direction.ASC)
                .named("idx_file_metadata_jobId"));

        // Index for status-based queries
        ops.ensureIndex(new Index()
                .on("status", Sort.Direction.ASC)
                .named("idx_file_metadata_status"));

        // TTL index: auto-delete file metadata after 90 days
        ops.ensureIndex(new Index()
                .on("uploadedAt", Sort.Direction.ASC)
                .expire(TTL_90_DAYS)
                .named("idx_file_metadata_ttl_90d"));

        log.debug("Indexes ensured for collection: {}", COLLECTION_FILE_METADATA);
    }
}
