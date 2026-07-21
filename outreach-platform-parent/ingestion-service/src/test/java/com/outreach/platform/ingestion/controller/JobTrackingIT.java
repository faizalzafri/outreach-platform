package com.outreach.platform.ingestion.controller;

import com.outreach.platform.ingestion.config.TestSecurityConfig;
import com.outreach.platform.ingestion.model.DomainEventDocument;
import com.outreach.platform.ingestion.model.EventStatus;
import com.outreach.platform.ingestion.model.JobStatus;
import com.outreach.platform.ingestion.model.JobTrackingDocument;
import com.outreach.platform.ingestion.repo.DomainEventRepository;
import com.outreach.platform.ingestion.repo.FileMetadataRepository;
import com.outreach.platform.ingestion.repo.JobTrackingRepository;
import com.outreach.platform.ingestion.service.DomainEventPublisher;
import com.outreach.platform.ingestion.service.JobTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for job lifecycle tracking and domain event publishing.
 * Verifies job status transitions (PENDING → RUNNING → COMPLETED/FAILED)
 * and domain event outbox writes against Testcontainers MongoDB.
 */
@Testcontainers
@ActiveProfiles("it")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        LiquibaseAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class JobTrackingIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @LocalServerPort
    private int port;

    @Inject
    private TestRestTemplate restTemplate;

    @Inject
    private JobTrackingService jobTrackingService;

    @Inject
    private JobTrackingRepository jobTrackingRepository;

    @Inject
    private FileMetadataRepository fileMetadataRepository;

    @Inject
    private DomainEventRepository domainEventRepository;

    @Inject
    private DomainEventPublisher domainEventPublisher;

    @BeforeEach
    void setUp() {
        jobTrackingRepository.deleteAll();
        fileMetadataRepository.deleteAll();
        domainEventRepository.deleteAll();
    }

    @Test
    void jobLifecycle_pendingToRunningToCompleted() {
        JobTrackingDocument created = jobTrackingService.createJob(
                "job-lifecycle-1", "FILE_IMPORT", "data.xlsx", ".xlsx", 1024L);
        assertThat(created.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(created.getProgress()).isZero();

        jobTrackingService.startJob("job-lifecycle-1", 100);
        Optional<JobTrackingDocument> running = jobTrackingRepository.findById("job-lifecycle-1");
        assertThat(running).isPresent();
        assertThat(running.get().getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(running.get().getTotalRows()).isEqualTo(100);
        assertThat(running.get().getStartedAt()).isNotNull();

        jobTrackingService.completeJob("job-lifecycle-1", 95, 5, List.of("Row 10: invalid email"));
        Optional<JobTrackingDocument> completed = jobTrackingRepository.findById("job-lifecycle-1");
        assertThat(completed).isPresent();
        assertThat(completed.get().getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(completed.get().getProgress()).isEqualTo(100);
        assertThat(completed.get().getProcessedRows()).isEqualTo(95);
        assertThat(completed.get().getErrorCount()).isEqualTo(5);
        assertThat(completed.get().getErrors()).contains("Row 10: invalid email");
        assertThat(completed.get().getCompletedAt()).isNotNull();
    }

    @Test
    void jobLifecycle_pendingToRunningToFailed() {
        jobTrackingService.createJob("job-fail-1", "FILE_IMPORT", "bad.csv", ".csv", 512L);

        jobTrackingService.startJob("job-fail-1", 50);
        jobTrackingService.failJob("job-fail-1", "Corrupted file at row 25");

        Optional<JobTrackingDocument> failed = jobTrackingRepository.findById("job-fail-1");
        assertThat(failed).isPresent();
        assertThat(failed.get().getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(failed.get().getErrors()).contains("Corrupted file at row 25");
        assertThat(failed.get().getCompletedAt()).isNotNull();
    }

    @Test
    void jobProgressUpdate_tracksRowsProcessed() {
        jobTrackingService.createJob("job-progress-1", "FILE_IMPORT", "large.xlsx", ".xlsx", 5000L);
        jobTrackingService.startJob("job-progress-1", 200);

        jobTrackingService.updateProgress("job-progress-1", 50, 200);
        Optional<JobTrackingDocument> mid = jobTrackingRepository.findById("job-progress-1");
        assertThat(mid).isPresent();
        assertThat(mid.get().getProgress()).isEqualTo(25);
        assertThat(mid.get().getProcessedRows()).isEqualTo(50);

        jobTrackingService.updateProgress("job-progress-1", 200, 200);
        Optional<JobTrackingDocument> done = jobTrackingRepository.findById("job-progress-1");
        assertThat(done).isPresent();
        assertThat(done.get().getProgress()).isEqualTo(100);
    }

    @Test
    void domainEventPublishing_writesToOutboxCollection() {
        domainEventPublisher.publishVolunteersImported("job-event-1", "volunteers.xlsx", 50);

        List<DomainEventDocument> events = domainEventRepository.findByStatus(EventStatus.PENDING);
        assertThat(events).hasSize(1);

        DomainEventDocument event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("VolunteersImported");
        assertThat(event.getStatus()).isEqualTo(EventStatus.PENDING);
        assertThat(event.getPayload()).containsEntry("jobId", "job-event-1");
        assertThat(event.getPayload()).containsEntry("fileName", "volunteers.xlsx");
        assertThat(event.getPayload()).containsEntry("importedCount", 50);
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    void domainEventPublishing_importJobCompletedEvent() {
        domainEventPublisher.publishImportJobCompleted(
                "job-event-2", "data.csv", "COMPLETED", 100, 95, 5);

        List<DomainEventDocument> events = domainEventRepository.findByStatus(EventStatus.PENDING);
        assertThat(events).hasSize(1);

        DomainEventDocument event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("ImportJobCompleted");
        assertThat(event.getPayload()).containsEntry("status", "COMPLETED");
        assertThat(event.getPayload()).containsEntry("totalRows", 100);
        assertThat(event.getPayload()).containsEntry("errorCount", 5);
    }

    @Test
    void getJobViaRestEndpoint_returnsJobDetails() {
        jobTrackingService.createJob("rest-job-1", "FILE_IMPORT", "test.xlsx", ".xlsx", 2048L);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ingestion/jobs/rest-job-1", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("id")).isEqualTo("rest-job-1");
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");
        assertThat(response.getBody().get("fileName")).isEqualTo("test.xlsx");
    }

    @Test
    void getJobNotFound_returns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/ingestion/jobs/nonexistent-job", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelPendingJob_returnsCancelledStatus() {
        jobTrackingService.createJob("cancel-job-1", "FILE_IMPORT", "cancel.xlsx", ".xlsx", 1024L);

        restTemplate.delete("/ingestion/jobs/cancel-job-1");

        Optional<JobTrackingDocument> cancelled = jobTrackingRepository.findById("cancel-job-1");
        assertThat(cancelled).isPresent();
        assertThat(cancelled.get().getStatus()).isEqualTo(JobStatus.CANCELLED);
        assertThat(cancelled.get().getCompletedAt()).isNotNull();
    }
}
