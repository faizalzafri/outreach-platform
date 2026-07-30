package com.outreach.platform.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Type-safe configuration properties for the AI Service, bound from the "platform.ai" prefix. */
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

    /** Per-feature toggle settings for AI capabilities. */
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

    /** Individual feature toggle. */
    public record FeatureToggle(boolean enabled) {}
}
