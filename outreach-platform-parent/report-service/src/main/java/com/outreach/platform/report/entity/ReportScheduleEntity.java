package com.outreach.platform.report.entity;

import com.outreach.platform.common.tenant.TenantConstants;
import com.outreach.platform.common.tenant.TenantEntityListener;
import com.outreach.platform.report.model.ExportFormat;
import com.outreach.platform.report.model.ScheduleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping to the report_schedules table.
 * Stores scheduled report configurations with cron-based execution.
 */
@Entity
@Table(name = "report_schedules")
@FilterDef(
        name = TenantConstants.TENANT_FILTER_NAME,
        parameters = @ParamDef(name = "tenantId", type = UUID.class)
)
@Filter(
        name = TenantConstants.TENANT_FILTER_NAME,
        condition = "tenant_id = :tenantId"
)
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
public class ReportScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_format", nullable = false, length = 20)
    private ExportFormat exportFormat;

    @Column(name = "filter_criteria", columnDefinition = "jsonb")
    private String filterCriteria;

    @Column(name = "recipients", columnDefinition = "jsonb")
    private String recipients;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScheduleStatus status;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    public ReportScheduleEntity() {
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

    void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public ExportFormat getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(ExportFormat exportFormat) {
        this.exportFormat = exportFormat;
    }

    public String getFilterCriteria() {
        return filterCriteria;
    }

    public void setFilterCriteria(String filterCriteria) {
        this.filterCriteria = filterCriteria;
    }

    public String getRecipients() {
        return recipients;
    }

    public void setRecipients(String recipients) {
        this.recipients = recipients;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Instant getNextRunAt() {
        return nextRunAt;
    }

    public void setNextRunAt(Instant nextRunAt) {
        this.nextRunAt = nextRunAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
