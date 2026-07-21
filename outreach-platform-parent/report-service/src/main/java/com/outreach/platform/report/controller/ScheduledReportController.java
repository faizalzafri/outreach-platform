package com.outreach.platform.report.controller;

import com.outreach.platform.report.model.ScheduledReportCreateRequest;
import com.outreach.platform.report.model.ScheduledReportDto;
import com.outreach.platform.report.model.ScheduledReportUpdateRequest;
import com.outreach.platform.report.service.ScheduledReportService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for scheduled report CRUD operations.
 * Manages cron-based report scheduling with configurable format and recipients.
 */
@RestController
@RequestMapping("/reports/scheduled")
public class ScheduledReportController {

    private final ScheduledReportService scheduledReportService;

    @Inject
    public ScheduledReportController(ScheduledReportService scheduledReportService) {
        this.scheduledReportService = scheduledReportService;
    }

    /**
     * Lists all scheduled report configurations.
     */
    @GetMapping
    public ResponseEntity<List<ScheduledReportDto>> listScheduledReports() {
        List<ScheduledReportDto> reports = scheduledReportService.listScheduledReports();
        return ResponseEntity.ok(reports);
    }

    /**
     * Creates a new scheduled report configuration.
     */
    @PostMapping
    public ResponseEntity<ScheduledReportDto> createScheduledReport(
            @Valid @RequestBody ScheduledReportCreateRequest request) {
        ScheduledReportDto created = scheduledReportService.createScheduledReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Updates an existing scheduled report configuration.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScheduledReportDto> updateScheduledReport(
            @PathVariable UUID id,
            @RequestBody ScheduledReportUpdateRequest request) {
        return scheduledReportService.updateScheduledReport(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a scheduled report configuration.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduledReport(@PathVariable UUID id) {
        if (scheduledReportService.deleteScheduledReport(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
