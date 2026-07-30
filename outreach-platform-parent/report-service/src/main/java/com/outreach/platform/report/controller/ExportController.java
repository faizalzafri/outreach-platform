package com.outreach.platform.report.controller;

import com.outreach.platform.report.model.ExportJobDto;
import com.outreach.platform.report.model.ExportJobStatus;
import com.outreach.platform.report.model.ExportRequest;
import com.outreach.platform.report.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for asynchronous report export operations.
 * Exports return 202 Accepted with a jobId for subsequent status polling.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Report Export", description = "Asynchronous report export to PDF, CSV, and Excel")
public class ExportController {

    private final ExportService exportService;

    @Inject
    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/export")
    public ResponseEntity<Map<String, String>> submitExport(@Valid @RequestBody ExportRequest request) {
        String jobId = exportService.submitExport(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("jobId", jobId));
    }

    /**
     * Gets the status of an export job and returns file content if completed.
     */
    @GetMapping("/export/{jobId}")
    public ResponseEntity<?> getExportStatus(@PathVariable String jobId) {
        ExportJobDto job = exportService.getExportStatus(jobId);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        if (job.status() == ExportJobStatus.COMPLETED) {
            byte[] content = exportService.getExportContent(jobId);
            if (content != null) {
                String contentType = resolveContentType(job);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + job.fileName() + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(content);
            }
        }

        return ResponseEntity.ok(job);
    }

    private String resolveContentType(ExportJobDto job) {
        return switch (job.format()) {
            case CSV -> "text/csv";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case PDF -> "application/pdf";
        };
    }
}
