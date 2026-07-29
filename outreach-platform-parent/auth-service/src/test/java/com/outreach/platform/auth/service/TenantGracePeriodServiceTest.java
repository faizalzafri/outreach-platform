package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.TenantProperties;
import com.outreach.platform.common.tenant.TenantConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TenantGracePeriodService}.
 * Uses a fixed clock to make date-based assertions deterministic.
 */
class TenantGracePeriodServiceTest {

    private static final LocalDate MIGRATION_START = LocalDate.of(2025, 1, 20);
    private static final int GRACE_PERIOD_DAYS = 30;

    private TenantGracePeriodService createService(LocalDate today) {
        Clock fixedClock = Clock.fixed(
                today.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        TenantProperties properties = new TenantProperties(GRACE_PERIOD_DAYS, MIGRATION_START);
        return new TenantGracePeriodService(properties, fixedClock);
    }

    @Test
    @DisplayName("isWithinGracePeriod returns true on migration start date")
    void withinGracePeriod_onMigrationStartDate() {
        TenantGracePeriodService service = createService(MIGRATION_START);
        assertTrue(service.isWithinGracePeriod());
    }

    @Test
    @DisplayName("isWithinGracePeriod returns true one day before expiration")
    void withinGracePeriod_oneDayBeforeExpiration() {
        LocalDate lastDay = MIGRATION_START.plusDays(GRACE_PERIOD_DAYS - 1);
        TenantGracePeriodService service = createService(lastDay);
        assertTrue(service.isWithinGracePeriod());
    }

    @Test
    @DisplayName("isWithinGracePeriod returns false on expiration date")
    void outsideGracePeriod_onExpirationDate() {
        LocalDate expirationDate = MIGRATION_START.plusDays(GRACE_PERIOD_DAYS);
        TenantGracePeriodService service = createService(expirationDate);
        assertFalse(service.isWithinGracePeriod());
    }

    @Test
    @DisplayName("isWithinGracePeriod returns false after expiration date")
    void outsideGracePeriod_afterExpiration() {
        LocalDate afterExpiration = MIGRATION_START.plusDays(GRACE_PERIOD_DAYS + 10);
        TenantGracePeriodService service = createService(afterExpiration);
        assertFalse(service.isWithinGracePeriod());
    }

    @Test
    @DisplayName("isWithinGracePeriod returns true before migration start date")
    void withinGracePeriod_beforeMigrationStart() {
        LocalDate beforeStart = MIGRATION_START.minusDays(5);
        TenantGracePeriodService service = createService(beforeStart);
        assertTrue(service.isWithinGracePeriod());
    }

    @Test
    @DisplayName("getDefaultTenantId returns the well-known Default Tenant UUID")
    void getDefaultTenantId_returnsConstant() {
        TenantGracePeriodService service = createService(MIGRATION_START);
        assertEquals(TenantConstants.DEFAULT_TENANT_ID, service.getDefaultTenantId());
    }

    @Test
    @DisplayName("Grace period of 0 days means expired immediately on start date")
    void zeroDayGracePeriod_expiredImmediately() {
        Clock fixedClock = Clock.fixed(
                MIGRATION_START.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()
        );
        TenantProperties properties = new TenantProperties(0, MIGRATION_START);
        TenantGracePeriodService service = new TenantGracePeriodService(properties, fixedClock);
        assertFalse(service.isWithinGracePeriod());
    }
}
