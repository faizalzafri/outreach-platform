package com.outreach.platform.event.entity;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.common.tenant.TenantEntityListener;
import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.EmailStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks volunteer enrollment and attendance for an event.
 */
@Entity
@Table(name = "event_enrollment", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "volunteer_id"})
})
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@FilterDef(name = TenantConstants.TENANT_FILTER_NAME, parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = TenantConstants.TENANT_FILTER_NAME, condition = "tenant_id = :tenantId")
public class EventEnrollmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private VolunteerEntity volunteer;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private AttendanceStatus attendanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    private EmailStatus emailStatus;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "attendance_marked_at")
    private Instant attendanceMarkedAt;

    @Column(name = "marked_by", length = 100)
    private String markedBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public EventEnrollmentEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public EventEntity getEvent() {
        return event;
    }

    public void setEvent(EventEntity event) {
        this.event = event;
    }

    public VolunteerEntity getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(VolunteerEntity volunteer) {
        this.volunteer = volunteer;
    }

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public EmailStatus getEmailStatus() {
        return emailStatus;
    }

    public void setEmailStatus(EmailStatus emailStatus) {
        this.emailStatus = emailStatus;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getAttendanceMarkedAt() {
        return attendanceMarkedAt;
    }

    public void setAttendanceMarkedAt(Instant attendanceMarkedAt) {
        this.attendanceMarkedAt = attendanceMarkedAt;
    }

    public String getMarkedBy() {
        return markedBy;
    }

    public void setMarkedBy(String markedBy) {
        this.markedBy = markedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
