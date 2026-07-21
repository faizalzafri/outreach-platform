package com.outreach.platform.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration properties for the AI Service.
 * Maps to the {@code platform.ai.*} namespace in application.yml.
 *
 * @param provider         AI provider identifier (e.g., "openai", "bedrock", "ollama")
 * @param apiKey           API key for the AI provider (resolved via environment or Secrets Manager)
 * @param model            AI model to use for completions
 * @param maxTokens        maximum tokens per AI request
 * @param temperature      sampling temperature for AI responses
 * @param jobResultTtlHours TTL in hours for AI job results stored in MongoDB
 * @param features         per-feature toggle configuration
 */
@ConfigurationProperties(prefix = "platform.ai")
public record AiServiceProperties(
        String provider,
        String apiKey,
        String model,
        int maxTokens,
        double temperature,
        int jobResultTtlHours,
        Features features
) {
    public AiServiceProperties {
        if (provider == null || provider.isBlank()) provider = "openai";
        if (model == null || model.isBlank()) model = "gpt-4o-mini";
        if (maxTokens <= 0) maxTokens = 4096;
        if (temperature <= 0.0) temperature = 0.7;
        if (jobResultTtlHours <= 0) jobResultTtlHours = 24;
        if (features == null) features = new Features(new FeatureToggle(false), new FeatureToggle(false), new FeatureToggle(false));
    }

    /**
     * Per-feature toggle settings for AI capabilities.
     *
     * @param summarize  summarization feature toggle
     * @param anomalies  anomaly detection feature toggle
     * @param query      natural language query feature toggle
     */
    public record Features(
            FeatureToggle summarize,
            FeatureToggle anomalies,
            FeatureToggle query
    ) {
        public Features {
            if (summarize == null) summarize = new FeatureToggle(false);
            if (anomalies == null) anomalies = new FeatureToggle(false);
            if (query == null) query = new FeatureToggle(false);
        }
    }

    /**
     * Individual feature toggle.
     *
     * @param enabled whether this AI feature is active
     */
    public record FeatureToggle(boolean enabled) {}
}
