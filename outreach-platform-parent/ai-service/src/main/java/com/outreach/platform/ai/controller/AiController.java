package com.outreach.platform.ai.controller;

import com.outreach.platform.ai.config.AiServiceProperties;
import com.outreach.platform.ai.entity.AiJobDocument;
import com.outreach.platform.ai.model.*;
import com.outreach.platform.ai.repo.AiJobRepository;
import com.outreach.platform.ai.service.AiService;
import com.outreach.platform.common.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/** REST controller for async AI operations (summarize, anomaly detection, NL queries). */
@RestController
@RequestMapping("/ai")
@Tag(name = "AI Operations", description = "AI-powered summarization, anomaly detection, and natural language queries (ROLE_ADMIN only)")
public class AiController {

    private final AiService aiService;
    private final AiJobRepository aiJobRepository;
    private final AiServiceProperties properties;

    @Inject
    public AiController(AiService aiService,
                        AiJobRepository aiJobRepository,
                        AiServiceProperties properties) {
        this.aiService = aiService;
        this.aiJobRepository = aiJobRepository;
        this.properties = properties;
    }

    @Operation(summary = "Summarize feedback", description = "Submits a feedback summarization job (async, returns 202 with jobId)")
    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@Valid @RequestBody SummarizeRequest request) {
        if (!properties.features().summarize().enabled()) {
            return featureDisabledResponse("summarize");
        }

        AiJobDocument job = createJob(AiJobType.SUMMARIZE, Map.of(
                "context", request.context(),
                "maxLength", request.maxLength() != null ? request.maxLength() : 500
        ));

        aiService.summarize(request).thenAccept(result -> completeJob(job, result));

        return acceptedResponse(job.getId());
    }

    @Operation(summary = "Detect anomalies", description = "Submits an anomaly detection job on feedback dataset (async, returns 202 with jobId)")
    @PostMapping("/anomalies")
    public ResponseEntity<?> detectAnomalies(@Valid @RequestBody AnomalyRequest request) {
        if (!properties.features().anomalies().enabled()) {
            return featureDisabledResponse("anomalies");
        }

        AiJobDocument job = createJob(AiJobType.ANOMALY_DETECTION, Map.of(
                "datasetSize", request.dataset().size(),
                "threshold", request.threshold() != null ? request.threshold() : 0.5
        ));

        aiService.detectAnomalies(request).thenAccept(result -> completeJob(job, result));

        return acceptedResponse(job.getId());
    }

    @Operation(summary = "Natural language query", description = "Submits a natural language query job (async, returns 202 with jobId)")
    @PostMapping("/query")
    public ResponseEntity<?> query(@Valid @RequestBody QueryRequest request) {
        if (!properties.features().query().enabled()) {
            return featureDisabledResponse("query");
        }

        AiJobDocument job = createJob(AiJobType.QUERY, Map.of(
                "prompt", request.prompt(),
                "context", request.context() != null ? request.context() : ""
        ));

        aiService.query(request).thenAccept(result -> completeJob(job, result));

        return acceptedResponse(job.getId());
    }

    @Operation(summary = "Get job result", description = "Retrieves AI job status and result by job ID")
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<AiJobResponse> getJobResult(@Parameter(description = "Job ID") @PathVariable String jobId) {
        // findById() alone would let any caller read any tenant's job result by ID — see
        // docs/specs/platform-hardening/ Finding 0 / Task 0.5.7. Empty TenantContext falls back to
        // the unscoped lookup only for the PLATFORM_ADMIN case (TenantContextFilter leaves it empty
        // when no X-Tenant-ID header is sent).
        var job = TenantContext.isPresent()
                ? aiJobRepository.findByIdAndTenantId(jobId, TenantContext.getCurrentTenantId())
                : aiJobRepository.findById(jobId);
        return job
                .map(j -> ResponseEntity.ok(new AiJobResponse(
                        j.getId(),
                        j.getStatus().name(),
                        j.getResult(),
                        j.getCreatedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "AI status", description = "Returns AI service health and feature capabilities status")
    @GetMapping("/status")
    public ResponseEntity<AiStatusResponse> status() {
        Map<String, Boolean> features = new LinkedHashMap<>();
        features.put("summarize", properties.features().summarize().enabled());
        features.put("anomalies", properties.features().anomalies().enabled());
        features.put("query", properties.features().query().enabled());

        String health = determineHealth();

        return ResponseEntity.ok(new AiStatusResponse(
                properties.provider(),
                features,
                health
        ));
    }

    private AiJobDocument createJob(AiJobType jobType, Map<String, Object> requestData) {
        Instant ttlExpires = Instant.now().plus(properties.jobResultTtlHours(), ChronoUnit.HOURS);
        AiJobDocument job = new AiJobDocument(TenantContext.getCurrentTenantId(), jobType, requestData, ttlExpires);
        return aiJobRepository.save(job);
    }

    private void completeJob(AiJobDocument job, AiJobResult result) {
        // Takes the AiJobDocument createJob() already returned rather than re-fetching by ID —
        // avoids a second, unscoped findById() call on a repository that (as of Task 0.5.7) is
        // tenant-scoped, and this way there's no tenant check to get right in the first place.
        if (result.success()) {
            job.setStatus(AiJobStatus.COMPLETED);
            job.setResult(result.content());
        } else {
            job.setStatus(AiJobStatus.FAILED);
            job.setResult(result.errorMessage());
        }
        job.setCompletedAt(Instant.now());
        aiJobRepository.save(job);
    }

    private ResponseEntity<?> featureDisabledResponse(String feature) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "AI_FEATURE_DISABLED");
        body.put("message", "The AI feature '" + feature + "' is currently disabled");
        body.put("feature", feature);
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(body);
    }

    private ResponseEntity<?> acceptedResponse(String jobId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jobId", jobId);
        body.put("status", AiJobStatus.PENDING.name());
        body.put("message", "Job submitted successfully");
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    private String determineHealth() {
        boolean anyFeatureEnabled = properties.features().summarize().enabled()
                || properties.features().anomalies().enabled()
                || properties.features().query().enabled();

        if (!anyFeatureEnabled) {
            return "DISABLED";
        }

        boolean hasApiKey = properties.apiKey() != null && !properties.apiKey().isBlank();
        return hasApiKey ? "UP" : "DEGRADED";
    }
}
