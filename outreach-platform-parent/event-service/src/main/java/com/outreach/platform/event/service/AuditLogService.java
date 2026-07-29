package com.outreach.platform.event.service;

import com.outreach.platform.event.model.AuditLogDocument;
import com.outreach.platform.event.model.PlatformAdminAuditDocument;
import com.outreach.platform.event.model.dto.AuditLogEntry;
import com.outreach.platform.event.model.dto.AuditLogSearchCriteria;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for writing and querying audit log entries in the MongoDB audit_logs collection.
 * Called explicitly by service-layer code after mutations for full transparency.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final String COLLECTION_AUDIT_LOGS = "audit_logs";
    private static final String COLLECTION_PLATFORM_ADMIN_AUDIT = "platform_admin_audit_logs";

    private final MongoTemplate mongoTemplate;

    @Inject
    public AuditLogService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Logs a Platform_Admin cross-tenant access event asynchronously (fire-and-forget).
     * <p>
     * Records the admin user, target tenant, action performed, endpoint accessed,
     * and any additional metadata to the {@code platform_admin_audit_logs} collection.
     *
     * @param adminUserId    the Platform_Admin user performing the cross-tenant access
     * @param targetTenantId the tenant being accessed (null if accessing all tenants)
     * @param action         the action performed (e.g., method name or operation type)
     * @param endpoint       the REST endpoint accessed
     * @param metadata       additional context (nullable)
     */
    @Async
    public void logCrossTenantAccess(String adminUserId, String targetTenantId,
                                     String action, String endpoint,
                                     Map<String, Object> metadata) {
        PlatformAdminAuditDocument doc = new PlatformAdminAuditDocument(
                adminUserId, targetTenantId, action, endpoint, metadata
        );
        mongoTemplate.save(doc, COLLECTION_PLATFORM_ADMIN_AUDIT);
        log.info("Platform_Admin cross-tenant access: admin={}, targetTenant={}, action={}, endpoint={}",
                adminUserId, targetTenantId, action, endpoint);
    }

    /**
     * Logs an audit entry for a mutation operation.
     *
     * @param userId       the user who performed the action
     * @param action       the action performed (e.g., "CREATE_EVENT", "UPDATE_STATUS")
     * @param resourceType the type of resource affected (e.g., "Event", "Volunteer")
     * @param resourceId   the identifier of the affected resource
     * @param details      additional context about the action (nullable)
     */
    public void log(String userId, String action, String resourceType,
                    String resourceId, Map<String, Object> details) {
        AuditLogDocument auditLog = new AuditLogDocument(userId, action, resourceType, resourceId, details);
        mongoTemplate.save(auditLog, COLLECTION_AUDIT_LOGS);
        log.debug("Audit log recorded: user={}, action={}, resource={}/{}",
                userId, action, resourceType, resourceId);
    }

    /**
     * Queries audit log entries with optional filters and pagination.
     *
     * @param criteria search filters (all optional)
     * @param pageable pagination parameters
     * @return paginated audit log entries
     */
    public Page<AuditLogEntry> queryAuditLogs(AuditLogSearchCriteria criteria, Pageable pageable) {
        Query query = buildQuery(criteria);

        long total = mongoTemplate.count(query, AuditLogDocument.class, COLLECTION_AUDIT_LOGS);
        query.with(pageable);

        List<AuditLogDocument> documents = mongoTemplate.find(query, AuditLogDocument.class, COLLECTION_AUDIT_LOGS);
        List<AuditLogEntry> entries = documents.stream()
                .map(this::toEntry)
                .collect(Collectors.toList());

        return new PageImpl<>(entries, pageable, total);
    }

    /**
     * Exports audit log entries matching the criteria as a CSV string.
     *
     * @param criteria search filters (all optional)
     * @return CSV-formatted string of matching audit log entries
     */
    public String exportAuditLogCsv(AuditLogSearchCriteria criteria) {
        Query query = buildQuery(criteria);
        List<AuditLogDocument> documents = mongoTemplate.find(query, AuditLogDocument.class, COLLECTION_AUDIT_LOGS);

        StringBuilder csv = new StringBuilder();
        csv.append("id,userId,action,resourceType,resourceId,timestamp\n");

        for (AuditLogDocument doc : documents) {
            csv.append(escapeCsv(doc.getId())).append(',')
                    .append(escapeCsv(doc.getUserId())).append(',')
                    .append(escapeCsv(doc.getAction())).append(',')
                    .append(escapeCsv(doc.getResourceType())).append(',')
                    .append(escapeCsv(doc.getResourceId())).append(',')
                    .append(doc.getTimestamp() != null ? doc.getTimestamp().toString() : "")
                    .append('\n');
        }

        return csv.toString();
    }

    private Query buildQuery(AuditLogSearchCriteria criteria) {
        Query query = new Query();

        if (criteria.userId() != null && !criteria.userId().isBlank()) {
            query.addCriteria(Criteria.where("userId").is(criteria.userId()));
        }
        if (criteria.action() != null && !criteria.action().isBlank()) {
            query.addCriteria(Criteria.where("action").is(criteria.action()));
        }
        if (criteria.resourceType() != null && !criteria.resourceType().isBlank()) {
            query.addCriteria(Criteria.where("resourceType").is(criteria.resourceType()));
        }
        if (criteria.dateFrom() != null && criteria.dateTo() != null) {
            query.addCriteria(Criteria.where("timestamp").gte(criteria.dateFrom()).lte(criteria.dateTo()));
        } else if (criteria.dateFrom() != null) {
            query.addCriteria(Criteria.where("timestamp").gte(criteria.dateFrom()));
        } else if (criteria.dateTo() != null) {
            query.addCriteria(Criteria.where("timestamp").lte(criteria.dateTo()));
        }

        return query;
    }

    private AuditLogEntry toEntry(AuditLogDocument doc) {
        return new AuditLogEntry(
                doc.getId(),
                doc.getUserId(),
                doc.getAction(),
                doc.getResourceType(),
                doc.getResourceId(),
                doc.getTimestamp(),
                doc.getDetails()
        );
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
