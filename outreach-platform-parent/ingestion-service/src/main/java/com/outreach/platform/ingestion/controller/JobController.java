package com.outreach.platform.ingestion.controller;

import com.outreach.platform.ingestion.model.JobTrackingDocument;
import com.outreach.platform.ingestion.service.JobTrackingService;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for job status, progress, error details, and cancellation.
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/ingestion/jobs")
public class JobController {

    private final JobTrackingService jobTrackingService;

    @Inject
    public JobController(JobTrackingService jobTrackingService) {
        this.jobTrackingService = jobTrackingService;
    }

    /**
     * List all jobs (paginated, sorted by createdAt desc).
     */
    @GetMapping
    public ResponseEntity<Page<JobTrackingDocument>> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<JobTrackingDocument> jobs = jobTrackingService.listJobs(PageRequest.of(page, size));
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get job status, progress, and errors.
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<JobTrackingDocument> getJob(@PathVariable String jobId) {
        return jobTrackingService.getJob(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel a pending or running job.
     */
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobId) {
        boolean cancelled = jobTrackingService.cancelJob(jobId);
        if (cancelled) {
            return ResponseEntity.ok(Map.of(
                    "jobId", jobId,
                    "status", "CANCELLED",
                    "message", "Job cancelled successfully"
            ));
        }
        return jobTrackingService.getJob(jobId)
                .map(job -> ResponseEntity.badRequest().body(Map.<String, Object>of(
                        "jobId", jobId,
                        "status", job.getStatus().name(),
                        "message", "Cannot cancel job in " + job.getStatus() + " state"
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get detailed error rows for a job.
     */
    @GetMapping("/{jobId}/errors")
    public ResponseEntity<Map<String, Object>> getJobErrors(@PathVariable String jobId) {
        return jobTrackingService.getJob(jobId)
                .map(job -> {
                    List<String> errors = job.getErrors();
                    Map<String, Object> body = Map.of(
                            "jobId", jobId,
                            "errorCount", job.getErrorCount(),
                            "errors", errors != null ? errors : List.of()
                    );
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
