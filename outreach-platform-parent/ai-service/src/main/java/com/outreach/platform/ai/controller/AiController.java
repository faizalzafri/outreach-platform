package com.outreach.platform.ai.controller;

import com.outreach.platform.ai.config.AiServiceProperties;
import com.outreach.platform.ai.entity.AiJobDocument;
import com.outreach.platform.ai.model.*;
import com.outreach.platform.ai.repo.AiJobRepository;
import com.outreach.platform.ai.service.AiService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for AI-powered operations.
 * All endpoints require ROLE_ADMIN (enforced via SecurityConfig).
 * Operations are async: they return 202 Accepted with a jobId.
 * Results are retrieved via GET /ai/jobs/{jobId}.
 */
@RestController
@RequestMapping("/ai")
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

    /**
     * Submit a summarization job.
     */
    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@Valid @RequestBody SummarizeRequest request) {
        if (!properties.features().summarize().enabled()) {
            return featureDisabledResponse("summarize");
        }

        AiJobDocument job = createJob(AiJobType.SUMMARIZE, Map.of(
                "context", request.context(),
                "maxLength", request.maxLength() != null ? request.maxLength() : 500
        ));

        aiService.summarize(request).thenAccept(result -> completeJob(job.getId(), result));

        return acceptedResponse(job.getId());
    }

    /**
     * Submit an anomaly detection job.
     */
    @PostMapping("/anomalies")
    public ResponseEntity<?> detectAnomalies(@Valid @RequestBody AnomalyRequest request) {
        if (!properties.features().anomalies().enabled()) {
            return featureDisabledResponse("anomalies");
        }

        AiJobDocument job = createJob(AiJobType.ANOMALY_DETECTION, Map.of(
                "datasetSize", request.dataset().size(),
                "threshold", request.threshold() != null ? request.threshold() : 0.5
        ));

        aiService.detectAnomalies(request).thenAccept(result -> completeJob(job.getId(), result));

        return acceptedResponse(job.getId());
    }

    /**
     * Submit a natural language query job.
     */
    @PostMapping("/query")
    public ResponseEntity<?> query(@Valid @RequestBody QueryRequest request) {
        if (!properties.features().query().enabled()) {
            return featureDisabledResponse("query");
        }

        AiJobDocument job = createJob(AiJobType.QUERY, Map.of(
                "prompt", request.prompt(),
                "context", request.context() != null ? request.context() : ""
        ));

        aiService.query(request).thenAccept(result -> completeJob(job.getId(), result));

        return acceptedResponse(job.getId());
    }

    /**
     * Retrieve AI job status and result.
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<AiJobResponse> getJobResult(@PathVariable String jobId) {
        return aiJobRepository.findById(jobId)
                .map(job -> ResponseEntity.ok(new AiJobResponse(
                        job.getId(),
                        job.getStatus().name(),
                        job.getResult(),
                        job.getCreatedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * AI service health and capabilities status.
     */
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
        AiJobDocument job = new AiJobDocument(jobType, requestData, ttlExpires);
        return aiJobRepository.save(job);
    }

    private void completeJob(String jobId, AiJobResult result) {
        aiJobRepository.findById(jobId).ifPresent(job -> {
            if (result.success()) {
                job.setStatus(AiJobStatus.COMPLETED);
                job.setResult(result.content());
            } else {
                job.setStatus(AiJobStatus.FAILED);
                job.setResult(result.errorMessage());
            }
            job.setCompletedAt(Instant.now());
            aiJobRepository.save(job);
        });
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
