package com.outreach.platform.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Discovery Service (Eureka Server).
 * Tests service registration and discovery functionality.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "eureka.client.register-with-eureka=false",
        "eureka.client.fetch-registry=false",
        "eureka.server.enable-self-preservation=false",
        "eureka.server.eviction-interval-timer-in-ms=1000"
})
class DiscoveryServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should return healthy status at actuator health endpoint without authentication")
    void shouldReturnHealthyStatus() {
        ResponseEntity<Map> response = restTemplate
                .getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("Should register a mock service instance and discover it in the registry")
    void shouldRegisterAndDiscoverService() throws InterruptedException {
        String appName = "TEST-SERVICE";

        // Register a mock instance via Eureka REST API
        String registrationXml = """
                <instance>
                    <instanceId>test-service:8080</instanceId>
                    <hostName>localhost</hostName>
                    <app>%s</app>
                    <ipAddr>127.0.0.1</ipAddr>
                    <status>UP</status>
                    <port enabled="true">8080</port>
                    <securePort enabled="false">443</securePort>
                    <dataCenterInfo class="com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo">
                        <name>MyOwn</name>
                    </dataCenterInfo>
                </instance>
                """.formatted(appName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        HttpEntity<String> request = new HttpEntity<>(registrationXml, headers);

        // Register the instance (Eureka REST endpoints are permitted without auth)
        ResponseEntity<String> registerResponse = restTemplate
                .postForEntity("/eureka/apps/" + appName, request, String.class);

        assertThat(registerResponse.getStatusCode())
                .as("Registration should succeed with 204 No Content")
                .isEqualTo(HttpStatus.NO_CONTENT);

        // Allow Eureka time to process the registration
        Thread.sleep(2000);

        // Discover the registered instance
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Void> jsonRequest = new HttpEntity<>(jsonHeaders);

        ResponseEntity<Map> discoveryResponse = restTemplate
                .exchange("/eureka/apps/" + appName, HttpMethod.GET, jsonRequest, Map.class);

        assertThat(discoveryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(discoveryResponse.getBody()).isNotNull();

        // Verify the application is in the registry
        Map<String, Object> application = (Map<String, Object>) discoveryResponse.getBody().get("application");
        assertThat(application).isNotNull();
        assertThat(application.get("name")).isEqualTo(appName);
    }

    @Test
    @DisplayName("Should return 404 for non-registered service lookup")
    void shouldReturn404ForUnknownService() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        // Use authenticated access since specific app lookups may behave differently
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("eurekaadmin", "eurekasecret")
                .exchange("/eureka/apps/NON-EXISTENT-SERVICE", HttpMethod.GET, request, String.class);

        // Eureka returns 404 when the app does not exist in the registry
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should allow unauthenticated access to Eureka endpoints")
    void shouldAllowUnauthenticatedAccessToEurekaEndpoints() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate
                .exchange("/eureka/apps", HttpMethod.GET, request, Map.class);

        // Eureka endpoints are public per security config
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
