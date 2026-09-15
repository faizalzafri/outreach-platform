package com.outreach.platform.event.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * MongoDB document for recording Platform_Admin cross-tenant access events.
 * <p>
 * Every time a Platform_Admin accesses data across tenants (either viewing all tenants
 * or targeting a specific tenant via {@code ?tenantId=}), an entry is persisted here
 * for compliance and security auditing.
 */
@Document(collection = "platform_admin_audit_logs")
@CompoundIndexes({
        @CompoundIndex(name = "idx_admin_timestamp", def = "{'adminUserId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "idx_target_tenant_timestamp", def = "{'targetTenantId': 1, 'timestamp': -1}")
})
public class PlatformAdminAuditDocument {

    @Id
    private String id;

    private String adminUserId;

    private String targetTenantId;

    private String action;

    private String endpoint;

    private Instant timestamp;

    private Map<String, Object> metadata;

    public PlatformAdminAuditDocument() {
    }

    public PlatformAdminAuditDocument(String adminUserId, String targetTenantId, String action,
                                       String endpoint, Map<String, Object> metadata) {
        this.id = UUID.randomUUID().toString();
        this.adminUserId = adminUserId;
        this.targetTenantId = targetTenantId;
        this.action = action;
        this.endpoint = endpoint;
        this.timestamp = Instant.now();
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(String adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getTargetTenantId() {
        return targetTenantId;
    }

    public void setTargetTenantId(String targetTenantId) {
        this.targetTenantId = targetTenantId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
