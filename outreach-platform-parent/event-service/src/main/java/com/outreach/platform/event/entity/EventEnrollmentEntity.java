package com.outreach.platform.event.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.event.model.AttendanceStatus;
import com.outreach.platform.event.model.EmailStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Tracks volunteer enrollment and attendance for an event.
 *
 * <p>See {@link EventEntity} for why the audit columns are overridden rather than renamed.
 * {@code updated_at}/{@code updated_by}/{@code version} were added by
 * {@code 20250122-004-add-missing-audit-columns.sql} specifically to allow this migration —
 * this entity had no equivalent columns before that.
 */
@Entity
@Table(name = "event_enrollment", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "volunteer_id"})
})
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class EventEnrollmentEntity extends TenantAwareBaseEntity {

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

    public EventEnrollmentEntity() {
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

    // id, tenantId, createdDate/lastModifiedDate/createdBy/lastModifiedBy, and version
    // are inherited — see the class-level @AttributeOverrides.
}
