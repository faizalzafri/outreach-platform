package com.outreach.platform.report.controller;

import com.outreach.platform.report.model.ScheduledReportCreateRequest;
import com.outreach.platform.report.model.ScheduledReportDto;
import com.outreach.platform.report.model.ScheduledReportUpdateRequest;
import com.outreach.platform.report.service.ScheduledReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Scheduled Reports", description = "CRUD operations for cron-based scheduled report configurations")
@PreAuthorize("hasAnyRole('PMO', 'ADMIN', 'TENANT_ADMIN', 'PLATFORM_ADMIN')")
public class ScheduledReportController {

    private final ScheduledReportService scheduledReportService;

    @Inject
    public ScheduledReportController(ScheduledReportService scheduledReportService) {
        this.scheduledReportService = scheduledReportService;
    }

    @GetMapping
    public ResponseEntity<List<ScheduledReportDto>> listScheduledReports() {
        List<ScheduledReportDto> reports = scheduledReportService.listScheduledReports();
        return ResponseEntity.ok(reports);
    }

    @PostMapping
    public ResponseEntity<ScheduledReportDto> createScheduledReport(
            @Valid @RequestBody ScheduledReportCreateRequest request) {
        ScheduledReportDto created = scheduledReportService.createScheduledReport(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledReportDto> updateScheduledReport(
            @PathVariable UUID id,
            @RequestBody ScheduledReportUpdateRequest request) {
        return scheduledReportService.updateScheduledReport(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScheduledReport(@PathVariable UUID id) {
        if (scheduledReportService.deleteScheduledReport(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
