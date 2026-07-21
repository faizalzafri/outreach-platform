package com.outreach.platform.ingestion;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import com.outreach.platform.ingestion.config.TestSecurityConfig;
import com.outreach.platform.ingestion.model.DomainEventDocument;
import com.outreach.platform.ingestion.model.EventStatus;
import com.outreach.platform.ingestion.repo.DomainEventRepository;
import com.outreach.platform.ingestion.repo.FileMetadataRepository;
import com.outreach.platform.ingestion.repo.JobTrackingRepository;
import com.outreach.platform.ingestion.service.DomainEventOutboxPoller;
import com.outreach.platform.ingestion.service.DomainEventPublisher;
import jakarta.inject.Inject;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test verifying the full domain event flow:
 * <ol>
 *   <li>Domain events written to MongoDB outbox (via DomainEventPublisher)</li>
 *   <li>Outbox poller picks up PENDING events and publishes to RabbitMQ</li>
 *   <li>Messages arrive on the correct queues (report and notification)</li>
 * </ol>
 * Also tests the full upload journey: file upload → job created → events published → messages delivered.
 *
 * Uses Testcontainers for MongoDB and RabbitMQ infrastructure.
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
@Import({TestSecurityConfig.class, EndToEndEventFlowIT.TestListenerConfig.class})
class EndToEndEventFlowIT {

    @Container
    static MongoDBContainer mongodb = new MongoDBContainer("mongo:7.0");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongodb::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @LocalServerPort
    private int port;

    @Inject
    private TestRestTemplate restTemplate;

    @Inject
    private DomainEventPublisher domainEventPublisher;

    @Inject
    private DomainEventOutboxPoller domainEventOutboxPoller;

    @Inject
    private DomainEventRepository domainEventRepository;

    @Inject
    private JobTrackingRepository jobTrackingRepository;

    @Inject
    private FileMetadataRepository fileMetadataRepository;

    @Inject
    private TestMessageCapture testMessageCapture;

    @BeforeEach
    void setUp() {
        domainEventRepository.deleteAll();
        jobTrackingRepository.deleteAll();
        fileMetadataRepository.deleteAll();
        testMessageCapture.reset();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Test 1: ImportJobCompleted → Report Queue
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void importJobCompleted_eventPublishedToReportQueue() throws InterruptedException {
        // Given: an ImportJobCompleted event is written to the outbox
        domainEventPublisher.publishImportJobCompleted(
                "e2e-job-1", "volunteers.xlsx", "COMPLETED", 100, 98, 2);

        // Verify outbox has a PENDING event
        List<DomainEventDocument> pending = domainEventRepository.findByStatus(EventStatus.PENDING);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getEventType()).isEqualTo("ImportJobCompleted");

        // When: the outbox poller runs
        domainEventOutboxPoller.pollAndPublish();

        // Then: the event is published to RabbitMQ and received on the report queue
        boolean received = testMessageCapture.reportLatch.await(10, TimeUnit.SECONDS);
        assertThat(received).as("Report queue should receive ImportJobCompleted event").isTrue();

        DomainEventMessage reportMsg = testMessageCapture.reportMessages.get(0);
        assertThat(reportMsg.eventType()).isEqualTo("ImportJobCompleted");
        assertThat(reportMsg.payload()).containsEntry("jobId", "e2e-job-1");
        assertThat(reportMsg.payload()).containsEntry("fileName", "volunteers.xlsx");
        assertThat(reportMsg.payload()).containsEntry("status", "COMPLETED");
        assertThat(reportMsg.payload()).containsEntry("totalRows", 100);
        assertThat(reportMsg.payload()).containsEntry("errorCount", 2);

        // Verify the event is marked as PUBLISHED in the outbox
        List<DomainEventDocument> published = domainEventRepository.findByStatus(EventStatus.PUBLISHED);
        assertThat(published).hasSize(1);
        assertThat(published.get(0).getPublishedAt()).isNotNull();
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Test 2: VolunteersImported → Notification Queue
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void volunteersImported_eventPublishedToNotificationQueue() throws InterruptedException {
        // Given: a VolunteersImported event is written to the outbox
        domainEventPublisher.publishVolunteersImported("e2e-job-2", "team.xlsx", 25);

        // When: the outbox poller runs
        domainEventOutboxPoller.pollAndPublish();

        // Then: the event is received on the notification queue
        boolean received = testMessageCapture.notificationLatch.await(10, TimeUnit.SECONDS);
        assertThat(received).as("Notification queue should receive VolunteersImported event").isTrue();

        DomainEventMessage notifMsg = testMessageCapture.notificationMessages.get(0);
        assertThat(notifMsg.eventType()).isEqualTo("VolunteersImported");
        assertThat(notifMsg.payload()).containsEntry("jobId", "e2e-job-2");
        assertThat(notifMsg.payload()).containsEntry("fileName", "team.xlsx");
        assertThat(notifMsg.payload()).containsEntry("importedCount", 25);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Test 3: Full journey - file upload → job tracking → events published → RabbitMQ
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    void fullJourney_uploadFile_jobTracked_eventsPublishedToQueues() throws Exception {
        // Given: a valid Excel file
        byte[] excelBytes = createValidExcelFile();

        // Step 1: Upload the file via REST endpoint
        ResponseEntity<Map> uploadResponse = uploadFile("e2e-volunteers.xlsx", excelBytes);

        // Verify upload is accepted and job is tracked
        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(uploadResponse.getBody()).isNotNull();
        String jobId = (String) uploadResponse.getBody().get("jobId");
        assertThat(jobId).isNotNull();
        assertThat(uploadResponse.getBody().get("status")).isEqualTo("ACCEPTED");

        // Step 2: Verify the job was created in MongoDB
        var jobOpt = jobTrackingRepository.findById(jobId);
        assertThat(jobOpt).isPresent();
        assertThat(jobOpt.get().getFileName()).isEqualTo("e2e-volunteers.xlsx");

        // Step 3: Simulate the import completion (publishing domain events as the processor would)
        domainEventPublisher.publishVolunteersImported(jobId, "e2e-volunteers.xlsx", 2);
        domainEventPublisher.publishImportJobCompleted(
                jobId, "e2e-volunteers.xlsx", "COMPLETED", 2, 2, 0);

        // Verify both events are in the outbox as PENDING
        List<DomainEventDocument> pendingEvents = domainEventRepository.findByStatus(EventStatus.PENDING);
        assertThat(pendingEvents).hasSize(2);

        // Step 4: Trigger the outbox poller to publish all pending events to RabbitMQ
        domainEventOutboxPoller.pollAndPublish();

        // Step 5: Verify messages arrive on both RabbitMQ queues
        boolean reportReceived = testMessageCapture.reportLatch.await(10, TimeUnit.SECONDS);
        boolean notificationReceived = testMessageCapture.notificationLatch.await(10, TimeUnit.SECONDS);

        assertThat(reportReceived)
                .as("Report queue should receive ImportJobCompleted event").isTrue();
        assertThat(notificationReceived)
                .as("Notification queue should receive VolunteersImported event").isTrue();

        // Verify report message contents
        DomainEventMessage reportMsg = testMessageCapture.reportMessages.get(0);
        assertThat(reportMsg.eventType()).isEqualTo("ImportJobCompleted");
        assertThat(reportMsg.payload()).containsEntry("jobId", jobId);

        // Verify notification message contents
        DomainEventMessage notifMsg = testMessageCapture.notificationMessages.get(0);
        assertThat(notifMsg.eventType()).isEqualTo("VolunteersImported");
        assertThat(notifMsg.payload()).containsEntry("jobId", jobId);
        assertThat(notifMsg.payload()).containsEntry("importedCount", 2);

        // Step 6: Verify all outbox events are marked PUBLISHED
        List<DomainEventDocument> stillPending = domainEventRepository.findByStatus(EventStatus.PENDING);
        assertThat(stillPending).isEmpty();

        List<DomainEventDocument> published = domainEventRepository.findByStatus(EventStatus.PUBLISHED);
        assertThat(published).hasSize(2);
        assertThat(published).allSatisfy(e -> assertThat(e.getPublishedAt()).isNotNull());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Test Configuration: RabbitListener that captures messages on both queues
    // ─────────────────────────────────────────────────────────────────────────────

    @TestConfiguration
    static class TestListenerConfig {

        @Bean
        public TestMessageCapture testMessageCapture() {
            return new TestMessageCapture();
        }
    }

    /**
     * Captures domain event messages arriving on both RabbitMQ queues for test verification.
     */
    static class TestMessageCapture {

        volatile CountDownLatch reportLatch = new CountDownLatch(1);
        volatile CountDownLatch notificationLatch = new CountDownLatch(1);
        final CopyOnWriteArrayList<DomainEventMessage> reportMessages = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<DomainEventMessage> notificationMessages = new CopyOnWriteArrayList<>();

        @RabbitListener(queues = RabbitMqConstants.QUEUE_REPORT)
        public void onReportMessage(DomainEventMessage message) {
            reportMessages.add(message);
            reportLatch.countDown();
        }

        @RabbitListener(queues = RabbitMqConstants.QUEUE_NOTIFICATION)
        public void onNotificationMessage(DomainEventMessage message) {
            notificationMessages.add(message);
            notificationLatch.countDown();
        }

        void reset() {
            reportLatch = new CountDownLatch(1);
            notificationLatch = new CountDownLatch(1);
            reportMessages.clear();
            notificationMessages.clear();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> uploadFile(String fileName, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(fileName, content));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity("/ingestion/upload", requestEntity, Map.class);
    }

    private byte[] createValidExcelFile() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Volunteers");
            Row header = sheet.createRow(0);
            String[] headers = {"employeeId", "fullName", "email", "phone",
                    "baseLocation", "department", "designation", "skills", "eventCode"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("EMP001");
            dataRow1.createCell(1).setCellValue("Alice Johnson");
            dataRow1.createCell(2).setCellValue("alice.johnson@example.com");
            dataRow1.createCell(3).setCellValue("555-0101");
            dataRow1.createCell(4).setCellValue("Bangalore");
            dataRow1.createCell(5).setCellValue("Engineering");
            dataRow1.createCell(6).setCellValue("Senior Developer");
            dataRow1.createCell(7).setCellValue("Java,Spring,Kafka");
            dataRow1.createCell(8).setCellValue("EVT-E2E-2024");

            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("EMP002");
            dataRow2.createCell(1).setCellValue("Bob Smith");
            dataRow2.createCell(2).setCellValue("bob.smith@example.com");
            dataRow2.createCell(3).setCellValue("555-0102");
            dataRow2.createCell(4).setCellValue("Chennai");
            dataRow2.createCell(5).setCellValue("QA");
            dataRow2.createCell(6).setCellValue("Lead QA");
            dataRow2.createCell(7).setCellValue("Selenium,Cypress");
            dataRow2.createCell(8).setCellValue("EVT-E2E-2024");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * ByteArrayResource subclass that provides a filename for multipart uploads.
     */
    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        NamedByteArrayResource(String filename, byte[] content) {
            super(content);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
