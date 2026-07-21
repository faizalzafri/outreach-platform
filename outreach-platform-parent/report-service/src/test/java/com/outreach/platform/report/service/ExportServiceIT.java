package com.outreach.platform.report.service;

import com.outreach.platform.report.model.ExportFormat;
import com.outreach.platform.report.model.ExportJobDto;
import com.outreach.platform.report.model.ExportJobStatus;
import com.outreach.platform.report.model.ExportRequest;
import com.outreach.platform.report.repo.ExportJobRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Inject
    private ExportService exportService;

    @Inject
    private ExportJobRepository exportJobRepository;

    @BeforeEach
    void cleanUp() {
        exportJobRepository.deleteAll();
    }

    @Test
    void submitExport_createsJobWithPendingStatus() {
        ExportRequest request = new ExportRequest(ExportFormat.CSV, Map.of("city", "Mumbai"));

        String jobId = exportService.submitExport(request);

        assertThat(jobId).isNotBlank();
        ExportJobDto status = exportService.getExportStatus(jobId);
        assertThat(status).isNotNull();
        assertThat(status.format()).isEqualTo(ExportFormat.CSV);
        // Status may have already transitioned if async is fast, so check it's at least created
        assertThat(status.status()).isIn(ExportJobStatus.PENDING, ExportJobStatus.RUNNING, ExportJobStatus.COMPLETED);
    }

    @Test
    void asyncExport_completesSuccessfully() {
        ExportRequest request = new ExportRequest(ExportFormat.CSV, Map.of());

        String jobId = exportService.submitExport(request);

        // Wait for async processing to complete
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ExportJobDto status = exportService.getExportStatus(jobId);
            assertThat(status).isNotNull();
            assertThat(status.status()).isEqualTo(ExportJobStatus.COMPLETED);
        });

        ExportJobDto completed = exportService.getExportStatus(jobId);
        assertThat(completed.fileName()).isNotBlank();
        assertThat(completed.completedAt()).isNotNull();
    }

    @Test
    void getExportContent_returnsBytesForCompletedJob() {
        ExportRequest request = new ExportRequest(ExportFormat.CSV, Map.of());

        String jobId = exportService.submitExport(request);

        // Wait for completion
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            ExportJobDto status = exportService.getExportStatus(jobId);
            assertThat(status.status()).isEqualTo(ExportJobStatus.COMPLETED);
        });

        byte[] content = exportService.getExportContent(jobId);

        assertThat(content).isNotNull();
        assertThat(content.length).isGreaterThan(0);
        // CSV export should contain header row
        String csv = new String(content);
        assertThat(csv).contains("EventId");
    }
}
