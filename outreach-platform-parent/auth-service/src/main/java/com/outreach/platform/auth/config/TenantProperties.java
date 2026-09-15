package com.outreach.platform.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * Configuration properties for tenant migration grace period behavior.
 *
 * @param gracePeriodDays    days after migration start to accept tokens without tenant_id (default: 30)
 * @param migrationStartDate the date when multi-tenant migration was deployed
 */
@ConfigurationProperties(prefix = "tenant")
@Validated
public record TenantProperties(

        @DefaultValue("30")
        @Min(0) int gracePeriodDays,

        @NotNull LocalDate migrationStartDate
) {
}
