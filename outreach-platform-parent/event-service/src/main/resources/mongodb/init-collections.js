/**
 * MongoDB Initialization Script for Outreach Platform
 *
 * This script creates collections and indexes for the outreach_nosql database.
 * Run with: mongosh mongodb://localhost:27017/outreach_nosql < init-collections.js
 *
 * Collections:
 *   - domain_events: Outbox pattern for inter-service domain events
 *   - audit_logs: Audit trail for mutations across all services
 *   - job_tracking: Excel import jobs, email dispatch batches, report exports
 *   - email_deliveries: Email delivery status tracking
 *   - analytics_snapshots: Pre-computed dashboard data
 *   - file_metadata: File processing state and metadata
 *
 * All TTL indexes use ISODate timestamps for automatic document expiration.
 */

// Switch to the target database
db = db.getSiblingDB('outreach_nosql');

print('=== Outreach Platform MongoDB Initialization ===');
print('Database: outreach_nosql');
print('');

// ─────────────────────────────────────────────────────────────────────────────
// Collection: domain_events
// Purpose: Outbox pattern for inter-service event publishing
// ─────────────────────────────────────────────────────────────────────────────
print('Creating collection: domain_events');
db.createCollection('domain_events');

// Compound index for outbox poller (find PENDING events ordered by creation time)
db.domain_events.createIndex(
  { status: 1, createdAt: 1 },
  { name: 'idx_domain_events_status_createdAt' }
);

// Index for querying by event type
db.domain_events.createIndex(
  { eventType: 1 },
  { name: 'idx_domain_events_eventType' }
);

// TTL index: auto-delete events after 30 days
db.domain_events.createIndex(
  { createdAt: 1 },
  { name: 'idx_domain_events_ttl_30d', expireAfterSeconds: 2592000 }
);

print('  ✓ domain_events indexes created');

// ─────────────────────────────────────────────────────────────────────────────
// Collection: audit_logs
// Purpose: Audit trail for all mutations across services
// ─────────────────────────────────────────────────────────────────────────────
print('Creating collection: audit_logs');
db.createCollection('audit_logs');

// Compound index for user activity queries (user + most recent first)
db.audit_logs.createIndex(
  { userId: 1, timestamp: -1 },
  { name: 'idx_audit_logs_userId_timestamp' }
);

// Index for querying by action type
db.audit_logs.createIndex(
  { action: 1 },
  { name: 'idx_audit_logs_action' }
);

// Compound index for resource-specific audit trail
db.audit_logs.createIndex(
  { resourceType: 1, resourceId: 1 },
  { name: 'idx_audit_logs_resource' }
);

// TTL index: auto-delete audit logs after 90 days
db.audit_logs.createIndex(
  { timestamp: 1 },
  { name: 'idx_audit_logs_ttl_90d', expireAfterSeconds: 7776000 }
);

print('  ✓ audit_logs indexes created');

// ─────────────────────────────────────────────────────────────────────────────
// Collection: job_tracking
// Purpose: Track Excel import jobs, email dispatch batches, report exports
// ─────────────────────────────────────────────────────────────────────────────
print('Creating collection: job_tracking');
db.createCollection('job_tracking');

// Compound index for status-based queries (active jobs, sorted by most recent)
db.job_tracking.createIndex(
  { status: 1, createdAt: -1 },
  { name: 'idx_job_tracking_status_createdAt' }
);

// Index for filtering by job type
db.job_tracking.createIndex(
  { jobType: 1 },
  { name: 'idx_job_tracking_jobType' }
);

// TTL index: auto-delete completed jobs after 60 days
db.job_tracking.createIndex(
  { createdAt: 1 },
  { name: 'idx_job_tracking_ttl_60d', expireAfterSeconds: 5184000 }
);

print('  ✓ job_tracking indexes created');

// ─────────────────────────────────────────────────────────────────────────────
// Collection: email_deliveries
// Purpose: Track email delivery status per recipient
// ─────────────────────────────────────────────────────────────────────────────
print('Creating collection: email_deliveries');
db.createCollection('email_deliveries');

// Compound index for event-specific delivery status queries
db.email_deliveries.createIndex(
  { eventId: 1, status: 1 },
  { name: 'idx_email_deliveries_eventId_status' }
);

// Index for recipient lookups
db.email_deliveries.createIndex(
  { recipientEmail: 1 },
  { name: 'idx_email_deliveries_recipientEmail' }
);

// Compound index for retry poller (find failed deliveries to retry)
db.email_deliveries.createIndex(
  { status: 1, lastAttemptAt: 1 },
  { name: 'idx_email_deliveries_status_lastAttemptAt' }
);

// TTL index: auto-delete delivery records after 90 days
db.email_deliveries.createIndex(
  { createdAt: 1 },
  { name: 'idx_email_deliveries_ttl_90d', expireAfterSeconds: 7776000 }
);

print('  ✓ email_deliveries indexes created');

// ─────────────────────────────────────────────────────────────────────────────
// Collection: analytics_snapshots
// Purpose: Pre-computed dashboard data and aggregation results
// ─────────────────────────────────────────────────────────────────────────────
print('Creating collection: analytics_snapshots');
db.createCollection('analytics_snapshots');

// Compound index for snapshot type queries (most recent first)
db.analytics_snapshots.createIndex(
  { snapshotType: 1, createdAt: -1 },
  { name: 'idx_analytics_snapshots_type_createdAt' }
);

// Index for event-specific snapshots
db.analytics_snapshots.createIndex(
  { eventId: 1 },
  { name: 'idx_analytics_snapshots_eventId' }
);

// TTL index: auto-delete snapshots after 365 days
db.analytics_snapshots.createIndex(
  { createdAt: 1 },
  { name: 'idx_analytics_snapshots_ttl_365d', expireAfterSeconds: 31536000 }
);

print('  ✓ analytics_snapshots indexes created');

// ─────────────────────────────────────────────────────────────────────────────
// Collection: file_metadata
// Purpose: File processing state and metadata for uploads
// ─────────────────────────────────────────────────────────────────────────────
print('Creating collection: file_metadata');
db.createCollection('file_metadata');

// Index for job-specific file lookups
db.file_metadata.createIndex(
  { jobId: 1 },
  { name: 'idx_file_metadata_jobId' }
);

// Index for status-based queries
db.file_metadata.createIndex(
  { status: 1 },
  { name: 'idx_file_metadata_status' }
);

// TTL index: auto-delete file metadata after 90 days
db.file_metadata.createIndex(
  { uploadedAt: 1 },
  { name: 'idx_file_metadata_ttl_90d', expireAfterSeconds: 7776000 }
);

print('  ✓ file_metadata indexes created');

// ─────────────────────────────────────────────────────────────────────────────
// Summary
// ─────────────────────────────────────────────────────────────────────────────
print('');
print('=== Initialization Complete ===');
print('Collections created: 6');
print('  - domain_events (3 indexes, TTL: 30 days)');
print('  - audit_logs (4 indexes, TTL: 90 days)');
print('  - job_tracking (3 indexes, TTL: 60 days)');
print('  - email_deliveries (4 indexes, TTL: 90 days)');
print('  - analytics_snapshots (3 indexes, TTL: 365 days)');
print('  - file_metadata (3 indexes, TTL: 90 days)');
