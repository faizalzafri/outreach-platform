package com.outreach.platform.event.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB document representing an audit log entry.
 * Captures mutations performed in the system for traceability.
 */
@Document(collection = "audit_logs")
public class AuditLogDocument {

    @Id
    private String id;
    private String userId;
    private String action;
    private String resourceType;
    private String resourceId;
    private Instant timestamp;
    private Map<String, Object> details;

    public AuditLogDocument() {
    }

    public AuditLogDocument(String userId, String action, String resourceType,
                            String resourceId, Map<String, Object> details) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.timestamp = Instant.now();
        this.details = details;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
