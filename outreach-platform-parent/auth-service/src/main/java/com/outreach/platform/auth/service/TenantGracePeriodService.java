package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.TenantProperties;
import com.outreach.platform.common.tenant.TenantConstants;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Service that determines whether the platform is within the tenant migration grace period.
 *
 * <p>During the grace period (from {@code migrationStartDate} to
 * {@code migrationStartDate + gracePeriodDays}), tokens without a {@code tenant_id} claim
 * are accepted and automatically assigned to the Default Tenant. After the grace period
 * expires, such tokens are rejected with {@code TOKEN_MIGRATION_EXPIRED}.
 *
 * <p>This service is consumed by the gateway's {@code TenantExtractionFilter} to decide
 * whether to allow or reject requests from tokens that predate the multi-tenant migration.
 *
 * @see TenantProperties
 * @see TenantConstants#DEFAULT_TENANT_ID
 */
@Service
public class TenantGracePeriodService {

    private final TenantProperties tenantProperties;
    private final Clock clock;

    /**
     * Constructs the grace period service with configuration properties and a system clock.
     *
     * @param tenantProperties tenant migration configuration
     * @param clock            clock for determining current date (supports testability)
     */
    @Inject
    public TenantGracePeriodService(TenantProperties tenantProperties, Clock clock) {
        this.tenantProperties = tenantProperties;
        this.clock = clock;
    }

    /**
     * Determines whether the current date is within the migration grace period.
     *
     * <p>Returns {@code true} if today is before {@code migrationStartDate + gracePeriodDays}.
     * During this window, tokens without a {@code tenant_id} claim should be accepted
     * and auto-assigned to the Default Tenant.
     *
     * @return {@code true} if the grace period has not yet expired
     */
    public boolean isWithinGracePeriod() {
        LocalDate today = LocalDate.now(clock);
        LocalDate expirationDate = tenantProperties.migrationStartDate()
                .plusDays(tenantProperties.gracePeriodDays());
        return today.isBefore(expirationDate);
    }

    /**
     * Returns the Default Tenant ID used for auto-assignment during the grace period.
     *
     * <p>Tokens without a {@code tenant_id} claim that arrive during the grace period
     * are treated as belonging to this tenant.
     *
     * @return the well-known Default Tenant UUID
     */
    public UUID getDefaultTenantId() {
        return TenantConstants.DEFAULT_TENANT_ID;
    }
}
