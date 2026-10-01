package com.outreach.platform.report.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.report.ReportServiceApplication;
import com.outreach.platform.report.model.ExportFormat;
import com.outreach.platform.report.model.ExportJobDto;
import com.outreach.platform.report.model.ExportJobStatus;
import com.outreach.platform.report.model.ExportRequest;
import com.outreach.platform.report.repo.ExportJobRepository;
import jakarta.inject.Inject;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

// Explicit classes=: bare @SpringBootTest auto-detects the nearest @SpringBootConfiguration by
// walking up from this test's own package, and RabbitMqEventListenerIT's nested TestApp (a
// minimal @SpringBootApplication for that class's own narrow test) lives in this exact same
// package — closer than the real ReportServiceApplication one package up — so it was being
// picked instead, booting a context with no ExportService bean at all.
@SpringBootTest(classes = ReportServiceApplication.class)
@ActiveProfiles("test")
@Testcontainers
class ExportServiceIT {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:6.0");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("reportdb")
            .withUsername("test")
            .withPassword("test");

    // The app has @RabbitListener beans that start eagerly on context refresh — without a real
    // broker, they either fail to connect or (worse, if something else is listening on the
    // default localhost:5672) fail auth against it.
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    // RedisCacheConfig's cacheManager bean requires a real RedisConnectionFactory.
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // application-test.yml excludes RedisAutoConfiguration globally (most tests don't need
        // real caching) — override back to nothing so RedisCacheConfig's cacheManager bean gets
        // a real RedisConnectionFactory from the container above.
        registry.add("spring.autoconfigure.exclude", () -> "");
    }


    private static final UUID TENANT_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID TENANT_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @Inject
    private ExportService exportService;

    @Inject
    private ExportJobRepository exportJobRepository;

    @Inject
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seed() {
        exportJobRepository.deleteAll();
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS events (
                    id BIGSERIAL PRIMARY KEY, event_name VARCHAR(255), city VARCHAR(100),
                    status VARCHAR(50), event_date DATE, tenant_id UUID)""");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS volunteer_feedback (
                    id BIGSERIAL PRIMARY KEY, event_id BIGINT REFERENCES events(id),
                    volunteer_id BIGINT, score INT, submitted_at TIMESTAMP DEFAULT NOW(), tenant_id UUID)""");
        jdbcTemplate.execute("DELETE FROM volunteer_feedback");
        jdbcTemplate.execute("DELETE FROM events");
        jdbcTemplate.update("INSERT INTO events (id, event_name, city, status, event_date, tenant_id) VALUES "
                + "(1, 'City Cleanup', 'Mumbai', 'COMPLETED', '2024-03-15', ?), "
                + "(2, '=Other Tenant Drive', 'Chennai', 'COMPLETED', '2024-03-20', ?)", TENANT_A, TENANT_B);
        jdbcTemplate.update("INSERT INTO volunteer_feedback (event_id, volunteer_id, score, tenant_id) VALUES "
                + "(1, 1, 5, ?), (1, 2, 3, ?), (2, 3, 1, ?)", TENANT_A, TENANT_A, TENANT_B);
        TenantContext.setCurrentTenantId(TENANT_A);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void submitExport_createsJobWithPendingStatus() {
        String jobId = exportService.submitExport(new ExportRequest(ExportFormat.CSV, Map.of("cities", List.of("Mumbai"))));

        assertThat(jobId).isNotBlank();
        ExportJobDto status = exportService.getExportStatus(jobId);
        assertThat(status).isNotNull();
        assertThat(status.format()).isEqualTo(ExportFormat.CSV);
        assertThat(status.status()).isIn(ExportJobStatus.PENDING, ExportJobStatus.RUNNING, ExportJobStatus.COMPLETED);
    }

    @Test
    void csvExport_containsOnlyTheSubmittingTenantsRows() {
        String csv = new String(export(ExportFormat.CSV, Map.of()), StandardCharsets.UTF_8);

        assertThat(csv).startsWith("\"Event ID\",\"Event Name\",\"City\",\"Average Score\"");
        assertThat(csv).contains("\"City Cleanup\",\"Mumbai\",\"4.00\",\"2\",\"3\",\"5\"");
        assertThat(csv).doesNotContain("Other Tenant Drive");
    }

    @Test
    void csvExport_neutralisesSpreadsheetFormulas() {
        TenantContext.setCurrentTenantId(TENANT_B);

        String csv = new String(export(ExportFormat.CSV, Map.of()), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=Other Tenant Drive\"");
    }

    @Test
    void excelExport_isARealWorkbook() throws Exception {
        byte[] xlsx = export(ExportFormat.EXCEL, Map.of("startDate", "2024-01-01", "endDate", "2024-12-31"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Event Name");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("City Cleanup");
            assertThat(sheet.getRow(1).getCell(3).getNumericCellValue()).isEqualTo(4.0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
        }
    }

    @Test
    void pdfExport_isARealPdf() {
        byte[] pdf = export(ExportFormat.PDF, Map.of());

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(500);
    }

    @Test
    void exportJob_isInvisibleToAnotherTenant() {
        String jobId = exportService.submitExport(new ExportRequest(ExportFormat.CSV, Map.of()));

        TenantContext.setCurrentTenantId(TENANT_B);
        assertThat(exportService.getExportStatus(jobId)).isNull();
        assertThat(exportService.getExportContent(jobId)).isNull();
    }

    /** Submits an export as the current tenant, waits for it to finish, and returns the file. */
    private byte[] export(ExportFormat format, Map<String, Object> filters) {
        String jobId = exportService.submitExport(new ExportRequest(format, filters));
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(exportService.getExportStatus(jobId).status()).isEqualTo(ExportJobStatus.COMPLETED));
        return exportService.getExportContent(jobId);
    }
}
