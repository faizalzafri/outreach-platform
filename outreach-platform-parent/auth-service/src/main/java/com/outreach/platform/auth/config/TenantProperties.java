package com.outreach.platform.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;

/**
 * Type-safe configuration properties for tenant migration and grace period behavior.
 *
 * <p>During the multi-tenant migration, tokens issued before the conversion may not contain
 * a {@code tenant_id} claim. The grace period allows these tokens to be accepted (with
 * automatic assignment to the Default Tenant) for a configurable number of days after
 * the migration start date.
 *
 * <p>After the grace period expires, tokens without a {@code tenant_id} claim are rejected
 * with {@code TOKEN_MIGRATION_EXPIRED}.
 *
 * <p>Configuration example:
 * <pre>
 * tenant:
 *   grace-period-days: 30
 *   migration-start-date: 2025-01-20
 * </pre>
 *
 * @param gracePeriodDays    number of days after migration start during which tokens without
 *                           a {@code tenant_id} claim are accepted (default: 30)
 * @param migrationStartDate the date when the multi-tenant migration was deployed
 * @see com.outreach.platform.auth.service.TenantGracePeriodService
 */
@ConfigurationProperties(prefix = "tenant")
@Validated
public record TenantProperties(

        @DefaultValue("30")
        @Min(0) int gracePeriodDays,

        @NotNull LocalDate migrationStartDate
) {
}
