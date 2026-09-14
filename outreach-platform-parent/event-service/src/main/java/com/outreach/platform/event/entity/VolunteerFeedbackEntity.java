package com.outreach.platform.event.entity;

import com.outreach.platform.common.tenant.TenantAwareBaseEntity;
import com.outreach.platform.event.model.FeedbackSentiment;
import com.outreach.platform.event.model.FeedbackStatus;
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
 * Volunteer feedback for a specific event including score and freeform answers.
 *
 * <p>See {@link EventEntity} for why the audit columns are overridden rather than renamed.
 */
@Entity
@Table(name = "volunteer_feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "volunteer_id"})
})
@AttributeOverride(name = "createdDate", column = @Column(name = "created_at", nullable = false, updatable = false))
@AttributeOverride(name = "lastModifiedDate", column = @Column(name = "updated_at"))
@AttributeOverride(name = "lastModifiedBy", column = @Column(name = "updated_by", length = 100))
public class VolunteerFeedbackEntity extends TenantAwareBaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private VolunteerEntity volunteer;

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

    @Column(name = "tags")
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment", length = 20)
    private FeedbackSentiment sentiment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
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

    // id, tenantId, createdDate/lastModifiedDate/createdBy/lastModifiedBy, and version
    // are inherited — see the class-level @AttributeOverrides.
}
