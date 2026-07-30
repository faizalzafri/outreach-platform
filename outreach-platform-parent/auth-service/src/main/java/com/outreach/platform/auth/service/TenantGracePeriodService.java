package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.TenantProperties;
import com.outreach.platform.common.tenant.TenantConstants;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/** Determines whether the platform is still within the tenant migration grace period. */
@Service
public class TenantGracePeriodService {

    private final TenantProperties tenantProperties;
    private final Clock clock;


    @Inject
    public TenantGracePeriodService(TenantProperties tenantProperties, Clock clock) {
        this.tenantProperties = tenantProperties;
        this.clock = clock;
    }

    /** Returns true if today is before migrationStartDate + gracePeriodDays. */
    public boolean isWithinGracePeriod() {
        LocalDate today = LocalDate.now(clock);
        LocalDate expirationDate = tenantProperties.migrationStartDate()
                .plusDays(tenantProperties.gracePeriodDays());
        return today.isBefore(expirationDate);
    }

    /** Returns the Default Tenant UUID used for auto-assignment during the grace period. */
    public UUID getDefaultTenantId() {
        return TenantConstants.DEFAULT_TENANT_ID;
    }
}
