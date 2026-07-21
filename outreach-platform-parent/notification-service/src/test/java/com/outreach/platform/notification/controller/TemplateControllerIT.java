package com.outreach.platform.notification.controller;

import com.outreach.platform.notification.model.NotificationType;
import com.outreach.platform.notification.model.TemplateEngine;
import com.outreach.platform.notification.model.dto.TemplateCreateRequest;
import com.outreach.platform.notification.model.dto.TemplateDto;
import com.outreach.platform.notification.model.dto.TemplatePreviewRequest;
import com.outreach.platform.notification.model.dto.TemplatePreviewResponse;
import com.outreach.platform.notification.model.dto.TemplateUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Template REST controller using Testcontainers.
 * Tests CRUD operations and template preview rendering.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class TemplateControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createTemplate_returnsCreatedWithId() {
        TemplateCreateRequest request = new TemplateCreateRequest(
                "welcome-email",
                NotificationType.EMAIL,
                "Welcome [[${name}]]!",
                "<p>Hello [[${name}]], welcome to the platform!</p>",
                TemplateEngine.THYMELEAF,
                null
        );

        ResponseEntity<TemplateDto> response = restTemplate.postForEntity(
                "/notifications/templates", request, TemplateDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("welcome-email");
        assertThat(response.getBody().type()).isEqualTo(NotificationType.EMAIL);
        assertThat(response.getBody().engine()).isEqualTo(TemplateEngine.THYMELEAF);
        assertThat(response.getBody().active()).isTrue();
    }

    @Test
    void listTemplates_returnsPageOfTemplates() {
        // Create a template first
        TemplateCreateRequest request = new TemplateCreateRequest(
                "list-test-template",
                NotificationType.EMAIL,
                "Subject",
                "<p>Body</p>",
                TemplateEngine.THYMELEAF,
                null
        );
        restTemplate.postForEntity("/notifications/templates", request, TemplateDto.class);

        // List templates
        ResponseEntity<RestPageResponse<TemplateDto>> response = restTemplate.exchange(
                "/notifications/templates",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isNotEmpty();
    }

    @Test
    void updateTemplate_modifiesExistingTemplate() {
        // Create
        TemplateCreateRequest createRequest = new TemplateCreateRequest(
                "update-test-template",
                NotificationType.EMAIL,
                "Old Subject",
                "<p>Old Body</p>",
                TemplateEngine.THYMELEAF,
                null
        );
        ResponseEntity<TemplateDto> createResponse = restTemplate.postForEntity(
                "/notifications/templates", createRequest, TemplateDto.class);
        UUID id = createResponse.getBody().id();

        // Update
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest(
                "updated-template-name",
                null,
                "New Subject [[${name}]]",
                "<p>New Body for [[${name}]]</p>",
                null,
                null,
                null
        );

        ResponseEntity<TemplateDto> updateResponse = restTemplate.exchange(
                "/notifications/templates/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                TemplateDto.class,
                id
        );

        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResponse.getBody()).isNotNull();
        assertThat(updateResponse.getBody().name()).isEqualTo("updated-template-name");
        assertThat(updateResponse.getBody().subjectTemplate()).isEqualTo("New Subject [[${name}]]");
        assertThat(updateResponse.getBody().bodyTemplate()).contains("New Body");
    }

    @Test
    void deleteTemplate_returnsNoContent() {
        // Create
        TemplateCreateRequest request = new TemplateCreateRequest(
                "delete-test-template",
                NotificationType.EMAIL,
                "Subject",
                "<p>Body</p>",
                TemplateEngine.THYMELEAF,
                null
        );
        ResponseEntity<TemplateDto> createResponse = restTemplate.postForEntity(
                "/notifications/templates", request, TemplateDto.class);
        UUID id = createResponse.getBody().id();

        // Delete
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/notifications/templates/{id}",
                HttpMethod.DELETE,
                null,
                Void.class,
                id
        );

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify deleted — attempt to update should return 404
        TemplateUpdateRequest updateRequest = new TemplateUpdateRequest(
                "ghost-template", null, null, null, null, null, null
        );
        ResponseEntity<String> updateResponse = restTemplate.exchange(
                "/notifications/templates/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest),
                String.class,
                id
        );
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void previewTemplate_rendersTemplateWithVariables() {
        // Create a template with Thymeleaf variables
        TemplateCreateRequest createRequest = new TemplateCreateRequest(
                "preview-test-template",
                NotificationType.EMAIL,
                "Hello [[${name}]]!",
                "<h1>Welcome, [[${name}]]!</h1><p>Your event: [[${eventName}]]</p>",
                TemplateEngine.THYMELEAF,
                null
        );
        ResponseEntity<TemplateDto> createResponse = restTemplate.postForEntity(
                "/notifications/templates", createRequest, TemplateDto.class);
        UUID id = createResponse.getBody().id();

        // Preview with variables
        TemplatePreviewRequest previewRequest = new TemplatePreviewRequest(
                id,
                Map.of("name", "Alice", "eventName", "Community Outreach")
        );

        ResponseEntity<TemplatePreviewResponse> previewResponse = restTemplate.postForEntity(
                "/notifications/templates/{id}/preview",
                previewRequest,
                TemplatePreviewResponse.class,
                id
        );

        assertThat(previewResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(previewResponse.getBody()).isNotNull();
        assertThat(previewResponse.getBody().renderedSubject()).contains("Alice");
        assertThat(previewResponse.getBody().renderedBody()).contains("Alice");
        assertThat(previewResponse.getBody().renderedBody()).contains("Community Outreach");
    }

    /**
     * Helper class to deserialize Spring Data Page responses.
     */
    static class RestPageResponse<T> {
        private java.util.List<T> content;
        private int totalPages;
        private long totalElements;
        private int number;
        private int size;

        public java.util.List<T> getContent() { return content; }
        public void setContent(java.util.List<T> content) { this.content = content; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
        public int getNumber() { return number; }
        public void setNumber(int number) { this.number = number; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }
}
