package com.outreach.platform.feedback.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapped to the volunteer_feedback table.
 * Represents a feedback submission from a volunteer for a specific event.
 * <p>
 * Extends {@link TenantAwareBaseEntity} which provides:
 * <ul>
 *   <li>UUID primary key (id)</li>
 *   <li>Tenant isolation via tenant_id column + Hibernate filter</li>
 *   <li>Automatic tenant assignment on persist via TenantEntityListener</li>
 *   <li>Audit fields (created_date, last_modified_date, created_by, last_modified_by)</li>
 *   <li>Optimistic locking (version)</li>
 * </ul>
 */
@Entity
@Table(name = "volunteer_feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "volunteer_id"})
})
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "createdBy", column = @Column(name = "created_by", length = 100))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class VolunteerFeedbackEntity extends TenantAwareBaseEntity {

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "volunteer_id", nullable = false)
    private UUID volunteerId;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "answer1", columnDefinition = "TEXT")
    private String answer1;

    @Column(name = "answer2", columnDefinition = "TEXT")
    private String answer2;

    @Column(name = "answer3", columnDefinition = "TEXT")
    private String answer3;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "tags", length = 255)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", length = 20)
    private FeedbackSentiment sentiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private FeedbackStatus status;

    @Column(name = "anonymous", nullable = false)
    private boolean anonymous;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    public VolunteerFeedbackEntity() {
    }

    // --- Getters and Setters ---

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(UUID volunteerId) {
        this.volunteerId = volunteerId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getAnswer1() {
        return answer1;
    }

    public void setAnswer1(String answer1) {
        this.answer1 = answer1;
    }

    public String getAnswer2() {
        return answer2;
    }

    public void setAnswer2(String answer2) {
        this.answer2 = answer2;
    }

    public String getAnswer3() {
        return answer3;
    }

    public void setAnswer3(String answer3) {
        this.answer3 = answer3;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public FeedbackSentiment getSentiment() {
        return sentiment;
    }

    public void setSentiment(FeedbackSentiment sentiment) {
        this.sentiment = sentiment;
    }

    public FeedbackStatus getStatus() {
        return status;
    }

    public void setStatus(FeedbackStatus status) {
        this.status = status;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public void setAnonymous(boolean anonymous) {
        this.anonymous = anonymous;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
}
