package com.outreach.platform.report.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * MongoDB document tracking the lifecycle of an asynchronous report export job.
 *
 * <p>Collection name is deliberately {@code export_job_tracking}, not {@code job_tracking} —
 * that name collides with ingestion-service's unrelated {@code JobTrackingDocument}, which shares
 * the same MongoDB database. The collision was discovered when ingestion-service's own job
 * inserts started failing with a duplicate-key error on this class's unique {@code jobId} index:
 * both document types were landing in the same physical collection, and ingestion-service's
 * documents (which have no {@code jobId} field at all) collided with each other under it.
 */
@Document("export_job_tracking")
public class ExportJobDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String jobId;

    private String jobType;

    private ExportFormat format;

    private ExportJobStatus status;

    private Map<String, Object> filterCriteria;

    private byte[] fileContent;

    private String fileName;

    private String errorMessage;

    private Instant createdAt;

    private Instant startedAt;

    private Instant completedAt;

    private String createdBy;

    public ExportJobDocument() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public ExportFormat getFormat() {
        return format;
    }

    public void setFormat(ExportFormat format) {
        this.format = format;
    }

    public ExportJobStatus getStatus() {
        return status;
    }

    public void setStatus(ExportJobStatus status) {
        this.status = status;
    }

    public Map<String, Object> getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(Map<String, Object> filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
