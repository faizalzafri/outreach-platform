package com.outreach.platform.event.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.event.model.EventStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Outreach event with full lifecycle management.
 *
 * <p>Audit column names predate {@link com.outreach.platform.common.entity.BaseEntity}'s
 * defaults ({@code created_at}/{@code updated_at}/{@code updated_by} vs. {@code created_date}/
 * {@code last_modified_date}/{@code last_modified_by}) — overridden below rather than migrating
 * the {@code events} table, since the column names themselves are correct and stable.
 */
@Entity
@Table(name = "events")
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class EventEntity extends TenantAwareBaseEntity {

    @Column(name = "event_code", nullable = false, unique = true, length = 100)
    private String eventCode;

    @Column(name = "event_name", nullable = false)
    private String eventName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EventStatus status;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "event_end_date")
    private LocalDate eventEndDate;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "venue")
    private String venue;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "max_volunteers")
    private Integer maxVolunteers;

    @Column(name = "registered_count")
    private Integer registeredCount;

    @Column(name = "attended_count")
    private Integer attendedCount;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    public EventEntity() {
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalDate getEventEndDate() {
        return eventEndDate;
    }

    public void setEventEndDate(LocalDate eventEndDate) {
        this.eventEndDate = eventEndDate;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getMaxVolunteers() {
        return maxVolunteers;
    }

    public void setMaxVolunteers(Integer maxVolunteers) {
        this.maxVolunteers = maxVolunteers;
    }

    public Integer getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(Integer registeredCount) {
        this.registeredCount = registeredCount;
    }

    public Integer getAttendedCount() {
        return attendedCount;
    }

    public void setAttendedCount(Integer attendedCount) {
        this.attendedCount = attendedCount;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Instant archivedAt) {
        this.archivedAt = archivedAt;
    }

    // id, tenantId, createdDate (created_at), lastModifiedDate (updated_at),
    // createdBy (created_by), lastModifiedBy (updated_by), and version are inherited
    // from TenantAwareBaseEntity/BaseEntity — see the class-level @AttributeOverrides
    // for the column-name mapping. Callers previously using getCreatedAt()/getUpdatedAt()/
    // getUpdatedBy() must switch to getCreatedDate()/getLastModifiedDate()/getLastModifiedBy().
}
