package com.outreach.platform.ai.model;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for anomaly detection in feedback datasets.
 *
 * @param dataset list of data points to analyze for anomalies
 * @param threshold optional sensitivity threshold (0.0 to 1.0)
 */
public record AnomalyRequest(
        @NotEmpty List<Map<String, Object>> dataset,
        Double threshold
) {}
