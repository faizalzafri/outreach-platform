package com.outreach.platform.notification.entity;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.common.tenant.TenantEntityListener;
import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to the notification_templates table.
 * Stores reusable notification templates with Thymeleaf/Freemarker body content.
 */
@Entity
@Table(name = "notification_templates")
@FilterDef(
        name = TenantConstants.TENANT_FILTER_NAME,
        parameters = @ParamDef(name = "tenantId", type = UUID.class)
)
@Filter(
        name = TenantConstants.TENANT_FILTER_NAME,
        condition = "tenant_id = :tenantId"
)
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
public class NotificationTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "subject_template", length = 100)
    private String subjectTemplate;

    @Column(name = "body_template", columnDefinition = "TEXT")
    private String bodyTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "engine", nullable = false, length = 50)
    private TemplateEngine engine;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables_schema", columnDefinition = "jsonb")
    private String variablesSchema;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private int version = 1;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public NotificationTemplateEntity() {
    }

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }

    public TemplateEngine getEngine() {
        return engine;
    }

    public void setEngine(TemplateEngine engine) {
        this.engine = engine;
    }

    public String getVariablesSchema() {
        return variablesSchema;
    }

    public void setVariablesSchema(String variablesSchema) {
        this.variablesSchema = variablesSchema;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
