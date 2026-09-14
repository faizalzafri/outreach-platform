package com.outreach.platform.report.integration;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.report.entity.ReportScheduleEntity;
import com.outreach.platform.report.model.ExportFormat;
import com.outreach.platform.report.model.ScheduleStatus;
import com.outreach.platform.report.repo.ReportScheduleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-tenant leakage regression test for {@link ReportScheduleEntity} (docs/specs/platform-hardening/
 * Task 0.5.6/1.4), run against a real Testcontainers Postgres with a test-only Liquibase changelog
 * (see {@code db.changelog-test.xml}, mirroring feedback-service's existing pattern for a service
 * that doesn't own its schema in production) and {@code ddl-auto=validate} — doubling as Task 1.5's
 * schema-drift validation, since report-service's own {@code application.yml} disables both
 * Liquibase and ddl-auto entirely (it's read-only-by-design; schema is owned by event-service).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"test", "postgres-it"})
@Testcontainers
class TenantScopedRepositoriesIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("report_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportScheduleRepository scheduleRepository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void crossTenantRead_returnsEmpty() {
        UUID tenantA = seedTenant();
        UUID tenantB = seedTenant();

        TenantContext.setCurrentTenantId(tenantA);
        ReportScheduleEntity saved = scheduleRepository.save(newSchedule("Tenant A Report"));

        TenantContext.setCurrentTenantId(tenantB);
        Optional<ReportScheduleEntity> readAsTenantB =
                scheduleRepository.findByIdAndTenantId(saved.getId(), tenantB);

        assertThat(readAsTenantB)
                .as("tenant B must not be able to read a report schedule created under tenant A")
                .isEmpty();
    }

    @Test
    void sameTenantRead_findsTheSchedule() {
        UUID tenantA = seedTenant();

        TenantContext.setCurrentTenantId(tenantA);
        ReportScheduleEntity saved = scheduleRepository.save(newSchedule("Tenant C Report"));

        Optional<ReportScheduleEntity> readBack =
                scheduleRepository.findByIdAndTenantId(saved.getId(), tenantA);

        assertThat(readBack).as("a tenant must still be able to read its own data").isPresent();
        assertThat(readBack.get().getTenantId()).isEqualTo(tenantA);
    }

    private UUID seedTenant() {
        // No TenantEntity/TenantRepository exists in this service — the tenants table only needs
        // a row to satisfy report_schedules' FK constraint, so a direct insert is simpler than
        // adding a JPA mapping just for this.
        UUID tenantId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, status) VALUES (?, ?, ?, 'ACTIVE')",
                tenantId, "Tenant " + tenantId, "tenant-" + tenantId);
        return tenantId;
    }

    private ReportScheduleEntity newSchedule(String name) {
        ReportScheduleEntity schedule = new ReportScheduleEntity();
        schedule.setName(name);
        schedule.setReportType("BY_EVENT");
        schedule.setCronExpression("0 0 * * *");
        schedule.setExportFormat(ExportFormat.PDF);
        schedule.setStatus(ScheduleStatus.ACTIVE);
        return schedule;
    }
}
