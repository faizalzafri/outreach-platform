package com.outreach.platform.event.entity;

import com.outreach.platform.common.pii.AesEncryptionConverter;
import com.outreach.platform.common.pii.PiiField;
import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.event.model.VolunteerAvailability;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Volunteer profile with encrypted PII fields.
 *
 * <p>See {@link EventEntity} for why the audit columns are overridden rather than renamed.
 */
@Entity
@Table(name = "volunteers")
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class VolunteerEntity extends TenantAwareBaseEntity {

    @Column(name = "employee_id", nullable = false, unique = true, length = 50)
    private String employeeId;

    @PiiField(description = "Volunteer full name")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "full_name_encrypted")
    private String fullName;

    @PiiField(description = "Volunteer email address")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "email_encrypted")
    private String email;

    @PiiField(description = "Volunteer phone number")
    @Convert(converter = AesEncryptionConverter.class)
    @Column(name = "phone_encrypted")
    private String phone;

    @Column(name = "base_location", length = 100)
    private String baseLocation;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "designation", length = 50)
    private String designation;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;

    @Enumerated(EnumType.STRING)
    @Column(name = "availability", length = 20)
    private VolunteerAvailability availability;

    @Column(name = "total_events_participated")
    private Integer totalEventsParticipated;

    @Column(name = "avg_feedback_score")
    private BigDecimal avgFeedbackScore;

    @Column(name = "last_participated_at")
    private Instant lastParticipatedAt;

    public VolunteerEntity() {
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public VolunteerAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(VolunteerAvailability availability) {
        this.availability = availability;
    }

    public Integer getTotalEventsParticipated() {
        return totalEventsParticipated;
    }

    public void setTotalEventsParticipated(Integer totalEventsParticipated) {
        this.totalEventsParticipated = totalEventsParticipated;
    }

    public BigDecimal getAvgFeedbackScore() {
        return avgFeedbackScore;
    }

    public void setAvgFeedbackScore(BigDecimal avgFeedbackScore) {
        this.avgFeedbackScore = avgFeedbackScore;
    }

    public Instant getLastParticipatedAt() {
        return lastParticipatedAt;
    }

    public void setLastParticipatedAt(Instant lastParticipatedAt) {
        this.lastParticipatedAt = lastParticipatedAt;
    }

    // id, tenantId, createdDate/lastModifiedDate/createdBy/lastModifiedBy, and version
    // are inherited — see the class-level @AttributeOverrides.
}
