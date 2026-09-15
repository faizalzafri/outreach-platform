package com.outreach.platform.notification.integration;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.notification.entity.NotificationScheduleEntity;
import com.outreach.platform.notification.entity.NotificationTemplateEntity;
import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.ScheduleStatus;
import com.outreach.platform.notification.model.TemplateEngine;
import com.outreach.platform.notification.model.TriggerType;
import com.outreach.platform.notification.repo.NotificationScheduleRepository;
import com.outreach.platform.notification.repo.NotificationTemplateRepository;
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
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cross-tenant leakage regression tests for {@link NotificationScheduleEntity}/
 * {@link NotificationTemplateEntity} (docs/specs/platform-hardening/ Task 0.5.6/1.4), run against
 * a real Testcontainers Postgres with Liquibase enabled and {@code ddl-auto=validate} — unlike
 * this service's default {@code application-test.yml} (H2, {@code create-drop}), this profile
 * doubles as Task 1.5's schema-drift validation: if either entity's mapping didn't match what
 * Liquibase actually produces, the Spring context would fail to start here, not just silently
 * pass against H2's auto-generated schema.
 *
 * <p>{@code notification_schedules}/{@code notification_templates} are normally created by
 * event-service's migrations against the shared schema; this service's own changelog only ever
 * had incremental ALTERs. {@code 20250120-001-ensure-notification-tables-exist.sql} (added
 * alongside this test) makes this service's changelog self-sufficient for isolated testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("postgres-it")
@Testcontainers
class TenantScopedRepositoriesIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    // The app has @RabbitListener beans that start eagerly on context refresh — without a real
    // broker, they either fail to connect or (worse, if something else is listening on the
    // default localhost:5672) fail auth against it.
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationScheduleRepository scheduleRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void templateRepository_crossTenantRead_returnsEmpty() {
        UUID tenantA = seedTenant();
        UUID tenantB = seedTenant();

        TenantContext.setCurrentTenantId(tenantA);
        NotificationTemplateEntity saved = templateRepository.save(newTemplate("tenant-a-template"));

        TenantContext.setCurrentTenantId(tenantB);
        Optional<NotificationTemplateEntity> readAsTenantB =
                templateRepository.findByIdAndTenantId(saved.getId(), tenantB);

        assertThat(readAsTenantB)
                .as("tenant B must not be able to read a template created under tenant A")
                .isEmpty();
    }

    @Test
    void templateRepository_sameTenantRead_findsTheTemplate() {
        UUID tenantA = seedTenant();

        TenantContext.setCurrentTenantId(tenantA);
        NotificationTemplateEntity saved = templateRepository.save(newTemplate("tenant-c-template"));

        Optional<NotificationTemplateEntity> readBack =
                templateRepository.findByIdAndTenantId(saved.getId(), tenantA);

        assertThat(readBack).as("a tenant must still be able to read its own data").isPresent();
        assertThat(readBack.get().getTenantId()).isEqualTo(tenantA);
    }

    @Test
    void scheduleRepository_crossTenantRead_returnsEmpty() {
        UUID tenantA = seedTenant();
        UUID tenantB = seedTenant();

        TenantContext.setCurrentTenantId(tenantA);
        NotificationScheduleEntity saved = scheduleRepository.save(newSchedule());

        TenantContext.setCurrentTenantId(tenantB);
        Optional<NotificationScheduleEntity> readAsTenantB =
                scheduleRepository.findByIdAndTenantId(saved.getId(), tenantB);

        assertThat(readAsTenantB)
                .as("tenant B must not be able to read a schedule created under tenant A")
                .isEmpty();
    }

    @Test
    void scheduleRepository_sameTenantRead_findsTheSchedule() {
        UUID tenantA = seedTenant();

        TenantContext.setCurrentTenantId(tenantA);
        NotificationScheduleEntity saved = scheduleRepository.save(newSchedule());

        Optional<NotificationScheduleEntity> readBack =
                scheduleRepository.findByIdAndTenantId(saved.getId(), tenantA);

        assertThat(readBack).as("a tenant must still be able to read its own data").isPresent();
        assertThat(readBack.get().getTenantId()).isEqualTo(tenantA);
    }

    private UUID seedTenant() {
        // No TenantEntity/TenantRepository exists in this service (unlike event-service) — the
        // tenants table only needs a row to satisfy notification_templates/notification_schedules'
        // FK constraint, so a direct insert is simpler than adding a JPA mapping just for this.
        UUID tenantId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO tenants (id, name, slug, status, created_by) VALUES (?, ?, ?, 'ACTIVE', 'test')",
                tenantId, "Tenant " + tenantId, "tenant-" + tenantId);
        return tenantId;
    }

    private NotificationTemplateEntity newTemplate(String name) {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setName(name + "-" + UUID.randomUUID());
        template.setType(NotificationType.EMAIL);
        template.setBodyTemplate("Hello {{name}}");
        template.setEngine(TemplateEngine.THYMELEAF);
        template.setActive(true);
        return template;
    }

    private NotificationScheduleEntity newSchedule() {
        NotificationScheduleEntity schedule = new NotificationScheduleEntity();
        schedule.setTemplateId(UUID.randomUUID());
        schedule.setTriggerType(TriggerType.IMMEDIATE);
        schedule.setStatus(ScheduleStatus.PENDING);
        return schedule;
    }
}
