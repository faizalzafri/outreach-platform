package com.outreach.platform.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for anomaly detection in feedback datasets.
 */
@Schema(description = "Request payload for anomaly detection in feedback datasets")
public record AnomalyRequest(
        @Schema(description = "List of data points to analyze for anomalies", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty List<Map<String, Object>> dataset,
        @Schema(description = "Sensitivity threshold (0.0 to 1.0)", example = "0.5", minimum = "0", maximum = "1")
        Double threshold
) {}
