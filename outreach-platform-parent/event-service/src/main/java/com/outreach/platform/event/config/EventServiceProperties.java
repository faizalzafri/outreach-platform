package com.outreach.platform.event.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe configuration properties for the Event Service.
 *
 * <p>Bound from the {@code event-service} prefix in application.yml:</p>
 * <pre>
 * event-service:
 *   max-events-per-page: 50
 *   default-page-size: 20
 *   event-code-prefix: EVT
 * </pre>
 */
@ConfigurationProperties(prefix = "event-service")
@Validated
public record EventServiceProperties(

        @Min(1) @Max(200)
        @DefaultValue("50")
        int maxEventsPerPage,

        @Min(1) @Max(100)
        @DefaultValue("20")
        int defaultPageSize,

        @NotBlank
        @DefaultValue("EVT")
        String eventCodePrefix
) {
}
