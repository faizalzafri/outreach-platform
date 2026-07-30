package com.outreach.platform.report.service;

import com.outreach.platform.report.model.ExportFormat;
import com.outreach.platform.report.model.ExportJobDocument;
import com.outreach.platform.report.model.ExportJobDto;
import com.outreach.platform.report.model.ExportJobStatus;
import com.outreach.platform.report.model.ExportRequest;
import com.outreach.platform.report.repo.ExportJobRepository;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service handling asynchronous report export generation.
 * Supports CSV, Excel, and PDF formats with job tracking in MongoDB.
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);

    private final ExportJobRepository exportJobRepository;

    @Inject
    public ExportService(ExportJobRepository exportJobRepository) {
        this.exportJobRepository = exportJobRepository;
    }

    /** Submits a new export job and triggers async generation. */
    public String submitExport(ExportRequest request) {
        String jobId = UUID.randomUUID().toString();

        ExportJobDocument job = new ExportJobDocument();
        job.setJobId(jobId);
        job.setJobType("REPORT_EXPORT");
        job.setFormat(request.format());
        job.setStatus(ExportJobStatus.PENDING);
        job.setFilterCriteria(request.filters());
        job.setCreatedAt(Instant.now());

        exportJobRepository.save(job);

        generateExportAsync(jobId, request.format(), request.filters());

        return jobId;
    }

    /** Retrieves the current status of an export job. */
    public ExportJobDto getExportStatus(String jobId) {
        return exportJobRepository.findByJobId(jobId)
                .map(this::toDto)
                .orElse(null);
    }

    /** Retrieves the file content bytes for a completed export job, or null if unavailable. */
    public byte[] getExportContent(String jobId) {
        return exportJobRepository.findByJobId(jobId)
                .filter(job -> job.getStatus() == ExportJobStatus.COMPLETED)
                .map(ExportJobDocument::getFileContent)
                .orElse(null);
    }

    @Async("exportTaskExecutor")
    public void generateExportAsync(String jobId, ExportFormat format, Map<String, Object> filters) {
        log.info("Starting async export generation for job: {}, format: {}", jobId, format);

        ExportJobDocument job = exportJobRepository.findByJobId(jobId).orElse(null);
        if (job == null) {
            log.error("Export job not found: {}", jobId);
            return;
        }

        job.setStatus(ExportJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        exportJobRepository.save(job);

        try {
            byte[] content = generateContent(format, filters);
            String fileName = buildFileName(format);

            job.setStatus(ExportJobStatus.COMPLETED);
            job.setFileContent(content);
            job.setFileName(fileName);
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);

            log.info("Export job completed: {}, fileName: {}", jobId, fileName);
        } catch (Exception e) {
            log.error("Export job failed: {}", jobId, e);
            job.setStatus(ExportJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            exportJobRepository.save(job);
        }
    }

    private byte[] generateContent(ExportFormat format, Map<String, Object> filters) {
        return switch (format) {
            case CSV -> generateCsvContent(filters);
            case EXCEL -> generateExcelContent(filters);
            case PDF -> generatePdfContent(filters);
        };
    }

    private byte[] generateCsvContent(Map<String, Object> filters) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.println("EventId,EventName,AvgScore,TotalFeedback,City");
            // Placeholder row — actual data fetching from read-only PostgreSQL will be
            // integrated when report queries are wired to the analytics layer
            writer.println("sample-event-id,Sample Event,4.2,15,Sample City");
        }
        return baos.toByteArray();
    }

    private byte[] generateExcelContent(Map<String, Object> filters) {
        // Apache POI Excel generation placeholder.
        // Full implementation will use XSSFWorkbook from poi-ooxml when added to dependencies.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            writer.println("EventId,EventName,AvgScore,TotalFeedback,City");
            writer.println("sample-event-id,Sample Event,4.2,15,Sample City");
        }
        return baos.toByteArray();
    }

    private byte[] generatePdfContent(Map<String, Object> filters) {
        // PDF generation placeholder.
        // Full implementation will use a PDF library (e.g., iText or OpenPDF).
        return "PDF report placeholder".getBytes(StandardCharsets.UTF_8);
    }

    private String buildFileName(ExportFormat format) {
        String timestamp = Instant.now().toString().replace(":", "-").replace(".", "-");
        return switch (format) {
            case CSV -> "report_" + timestamp + ".csv";
            case EXCEL -> "report_" + timestamp + ".xlsx";
            case PDF -> "report_" + timestamp + ".pdf";
        };
    }

    private ExportJobDto toDto(ExportJobDocument doc) {
        return new ExportJobDto(
                doc.getJobId(),
                doc.getStatus(),
                doc.getFormat(),
                doc.getFileName(),
                doc.getErrorMessage(),
                doc.getCreatedAt(),
                doc.getCompletedAt()
        );
    }
}
