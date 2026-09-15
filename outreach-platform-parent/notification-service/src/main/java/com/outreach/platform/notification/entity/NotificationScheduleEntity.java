package com.outreach.platform.notification.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.notification.model.ScheduleStatus;
import com.outreach.platform.notification.model.TriggerType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a notification schedule configuration.
 */
@Entity
@Table(name = "notification_schedules")
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class NotificationScheduleEntity extends TenantAwareBaseEntity {

    @Column(name = "template_id", nullable = false)
    private UUID templateId;

    @Column(name = "event_id")
    private UUID eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private TriggerType triggerType;

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipient_filter", columnDefinition = "jsonb")
    private String recipientFilter;

    public NotificationScheduleEntity() {
    }

    // --- Getters and Setters ---
    // id/tenantId/createdDate(→created_at)/lastModifiedDate(→updated_at)/createdBy/
    // lastModifiedBy(→updated_by)/version are inherited from TenantAwareBaseEntity/BaseEntity —
    // see the class-level @AttributeOverrides for the column-name mapping. Callers previously
    // using getCreatedAt() must switch to getCreatedDate().

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(TriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(Instant scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public String getRecipientFilter() {
        return recipientFilter;
    }

    public void setRecipientFilter(String recipientFilter) {
        this.recipientFilter = recipientFilter;
    }
}
