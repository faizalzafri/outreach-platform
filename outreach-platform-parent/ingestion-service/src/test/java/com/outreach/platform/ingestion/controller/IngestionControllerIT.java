package com.outreach.platform.ingestion.controller;

import com.outreach.platform.ingestion.config.TestSecurityConfig;
import com.outreach.platform.ingestion.model.JobTrackingDocument;
import com.outreach.platform.ingestion.repo.DomainEventRepository;
import com.outreach.platform.ingestion.repo.FileMetadataRepository;
import com.outreach.platform.ingestion.repo.JobTrackingRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the IngestionController endpoints using Testcontainers MongoDB.
 * Covers file upload, validation, template download, and error handling.
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
class IngestionControllerIT {

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
    private JobTrackingRepository jobTrackingRepository;

    @Inject
    private FileMetadataRepository fileMetadataRepository;

    @Inject
    private DomainEventRepository domainEventRepository;

    @BeforeEach
    void setUp() {
        jobTrackingRepository.deleteAll();
        fileMetadataRepository.deleteAll();
        domainEventRepository.deleteAll();
    }

    @Test
    void uploadValidExcelFile_returns202WithJobId() throws IOException {
        byte[] excelBytes = createValidExcelFile();

        ResponseEntity<Map> response = uploadFile("volunteers.xlsx", excelBytes);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("jobId")).isNotNull();
        assertThat(response.getBody().get("fileName")).isEqualTo("volunteers.xlsx");
        assertThat(response.getBody().get("status")).isEqualTo("ACCEPTED");
    }

    @Test
    void uploadEmptyFile_returns400() {
        byte[] emptyBytes = new byte[0];

        ResponseEntity<Map> response = uploadFile("empty.xlsx", emptyBytes);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("BAD_REQUEST");
    }

    @Test
    void uploadUnsupportedExtension_returns400() {
        byte[] content = "some content".getBytes(StandardCharsets.UTF_8);

        ResponseEntity<Map> response = uploadFile("data.pdf", content);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("BAD_REQUEST");
        assertThat((String) response.getBody().get("message")).contains("Unsupported file extension");
    }

    @Test
    void validateValidCsv_returnsZeroErrors() {
        String csv = buildValidCsv();

        ResponseEntity<Map> response = validateFile("valid.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("errorCount")).isEqualTo(0);
        assertThat((int) response.getBody().get("validCount")).isGreaterThan(0);
    }

    @Test
    void validateInvalidCsv_returnsStructuredErrors() {
        String csv = buildInvalidCsv();

        ResponseEntity<Map> response = validateFile("invalid.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat((int) response.getBody().get("errorCount")).isGreaterThan(0);
        assertThat(response.getBody().get("errors")).isNotNull();
    }

    @Test
    void downloadTemplate_returnsXlsxWithCorrectHeaders() {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                "/ingestion/templates", byte[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString())
                .contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
    }

    @Test
    void uploadCreatesJobDocumentInMongoDB() throws IOException {
        byte[] excelBytes = createValidExcelFile();

        ResponseEntity<Map> response = uploadFile("test-upload.xlsx", excelBytes);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        String jobId = (String) response.getBody().get("jobId");
        assertThat(jobId).isNotNull();

        var jobOpt = jobTrackingRepository.findById(jobId);
        assertThat(jobOpt).isPresent();

        JobTrackingDocument job = jobOpt.get();
        assertThat(job.getStatus().name()).isEqualTo("PENDING");
        assertThat(job.getFileName()).isEqualTo("test-upload.xlsx");
        assertThat(job.getJobType()).isEqualTo("FILE_IMPORT");
    }

    // --- Helper methods ---

    private ResponseEntity<Map> uploadFile(String fileName, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(fileName, content));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity("/ingestion/upload", requestEntity, Map.class);
    }

    private ResponseEntity<Map> validateFile(String fileName, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(fileName, content));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity("/ingestion/validate", requestEntity, Map.class);
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

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("EMP001");
            dataRow.createCell(1).setCellValue("John Doe");
            dataRow.createCell(2).setCellValue("john.doe@example.com");
            dataRow.createCell(3).setCellValue("123-456-7890");
            dataRow.createCell(4).setCellValue("Bangalore");
            dataRow.createCell(5).setCellValue("Engineering");
            dataRow.createCell(6).setCellValue("Senior Developer");
            dataRow.createCell(7).setCellValue("Java,Spring");
            dataRow.createCell(8).setCellValue("EVT-2024");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private String buildValidCsv() {
        return """
                employeeId,fullName,email,phone,baseLocation,department,designation,skills,eventCode
                EMP001,John Doe,john.doe@example.com,123-456-7890,Bangalore,Engineering,Senior Dev,Java,EVT-2024
                EMP002,Jane Smith,jane.smith@example.com,987-654-3210,Chennai,QA,Lead QA,Selenium,EVT-2024
                """;
    }

    private String buildInvalidCsv() {
        return """
                employeeId,fullName,email,phone,baseLocation,department,designation,skills,eventCode
                ,Missing Name,,invalid-phone,,,,,
                EMP@123,John Doe,not-an-email,,,,,, 
                """;
    }

    /**
     * ByteArrayResource with a filename for multipart upload support.
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
