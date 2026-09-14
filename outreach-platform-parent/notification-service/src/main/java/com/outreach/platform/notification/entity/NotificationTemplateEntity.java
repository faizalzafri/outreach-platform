package com.outreach.platform.notification.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.JdbcTypeCode;

/**
 * JPA entity representing a reusable notification template.
 */
@Entity
@Table(name = "notification_templates")
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class NotificationTemplateEntity extends TenantAwareBaseEntity {

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

    public NotificationTemplateEntity() {
    }

    // --- Getters and Setters ---
    // id/tenantId/createdDate(→created_at)/lastModifiedDate(→updated_at)/createdBy/
    // lastModifiedBy(→updated_by)/version are inherited from TenantAwareBaseEntity/BaseEntity —
    // see the class-level @AttributeOverrides for the column-name mapping. Callers previously
    // using getCreatedAt()/getUpdatedAt() must switch to getCreatedDate()/getLastModifiedDate().

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
}
