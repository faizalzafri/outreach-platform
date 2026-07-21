package com.outreach.platform.ai.entity;

import com.outreach.platform.ai.model.AiJobStatus;
import com.outreach.platform.ai.model.AiJobType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document representing an AI job.
 * TTL is managed via the ttlExpiresAt field with a MongoDB TTL index.
 */
@Document(collection = "ai_jobs")
public class AiJobDocument {

    @Id
    private String id;

    private AiJobType jobType;

    private AiJobStatus status;

    private Map<String, Object> request;

    private String result;

    private Instant createdAt;

    private Instant completedAt;

    @Indexed(expireAfter = "0s")
    private Instant ttlExpiresAt;

    public AiJobDocument() {}

    public AiJobDocument(AiJobType jobType, Map<String, Object> request, Instant ttlExpiresAt) {
        this.jobType = jobType;
        this.status = AiJobStatus.PENDING;
        this.request = request;
        this.createdAt = Instant.now();
        this.ttlExpiresAt = ttlExpiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public AiJobType getJobType() {
        return jobType;
    }

    public void setJobType(AiJobType jobType) {
        this.jobType = jobType;
    }

    public AiJobStatus getStatus() {
        return status;
    }

    public void setStatus(AiJobStatus status) {
        this.status = status;
    }

    public Map<String, Object> getRequest() {
        return request;
    }

    public void setRequest(Map<String, Object> request) {
        this.request = request;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getTtlExpiresAt() {
        return ttlExpiresAt;
    }

    public void setTtlExpiresAt(Instant ttlExpiresAt) {
        this.ttlExpiresAt = ttlExpiresAt;
    }
}
