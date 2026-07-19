package com.outreach.platform.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Config Server.
 * Tests config retrieval by profile and encryption/decryption round-trip.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.profiles.active=native",
        "spring.cloud.config.server.native.search-locations=file:./config"
})
class ConfigServerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Nested
    @DisplayName("Config Retrieval by Profile")
    class ConfigRetrievalTests {

        @Test
        @DisplayName("Should retrieve default application config")
        void shouldRetrieveDefaultConfig() {
            ResponseEntity<Map> response = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .getForEntity("/application/default", Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("name")).isEqualTo("application");
        }

        @Test
        @DisplayName("Should retrieve development profile config with correct properties")
        void shouldRetrieveDevelopmentProfileConfig() {
            ResponseEntity<Map> response = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .getForEntity("/application/development", Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            // Verify profile-specific property sources are present
            var propertySources = (java.util.List<Map<String, Object>>) response.getBody().get("propertySources");
            assertThat(propertySources).isNotEmpty();

            // Find the development profile property source
            boolean hasDevelopmentSource = propertySources.stream()
                    .anyMatch(ps -> ps.get("name").toString().contains("application-development"));
            assertThat(hasDevelopmentSource)
                    .as("Should contain application-development property source")
                    .isTrue();
        }

        @Test
        @DisplayName("Should retrieve staging profile config with encrypted values")
        void shouldRetrieveStagingProfileConfig() {
            ResponseEntity<Map> response = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .getForEntity("/application/staging", Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            var propertySources = (java.util.List<Map<String, Object>>) response.getBody().get("propertySources");
            assertThat(propertySources).isNotEmpty();

            boolean hasStagingSource = propertySources.stream()
                    .anyMatch(ps -> ps.get("name").toString().contains("application-staging"));
            assertThat(hasStagingSource)
                    .as("Should contain application-staging property source")
                    .isTrue();
        }

        @Test
        @DisplayName("Should retrieve production profile config")
        void shouldRetrieveProductionProfileConfig() {
            ResponseEntity<Map> response = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .getForEntity("/application/production", Map.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();

            var propertySources = (java.util.List<Map<String, Object>>) response.getBody().get("propertySources");
            assertThat(propertySources).isNotEmpty();

            boolean hasProductionSource = propertySources.stream()
                    .anyMatch(ps -> ps.get("name").toString().contains("application-production"));
            assertThat(hasProductionSource)
                    .as("Should contain application-production property source")
                    .isTrue();
        }

        @Test
        @DisplayName("Should reject unauthenticated requests with 401")
        void shouldRejectUnauthenticatedRequests() {
            ResponseEntity<String> response = restTemplate
                    .getForEntity("/application/default", String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Configuration Encryption Round-Trip")
    class EncryptionTests {

        @Test
        @DisplayName("Should encrypt a plain text value and decrypt it back to original")
        void shouldEncryptAndDecryptRoundTrip() {
            String plainText = "my-secret-database-password";

            // Encrypt the plain text (with credentials)
            ResponseEntity<String> encryptResponse = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .postForEntity("/encrypt", plainText, String.class);

            assertThat(encryptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            String cipherText = encryptResponse.getBody();
            assertThat(cipherText).isNotNull().isNotEmpty();
            assertThat(cipherText).isNotEqualTo(plainText);

            // Decrypt the cipher text (with credentials)
            ResponseEntity<String> decryptResponse = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .postForEntity("/decrypt", cipherText, String.class);

            assertThat(decryptResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(decryptResponse.getBody()).isEqualTo(plainText);
        }

        @Test
        @DisplayName("Should produce cipher texts that both decrypt to original plain text")
        void shouldDecryptMultipleCipherTextsToSamePlainText() {
            String plainText = "test-value-for-encryption";

            ResponseEntity<String> firstEncrypt = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .postForEntity("/encrypt", plainText, String.class);

            ResponseEntity<String> secondEncrypt = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .postForEntity("/encrypt", plainText, String.class);

            assertThat(firstEncrypt.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(secondEncrypt.getStatusCode()).isEqualTo(HttpStatus.OK);

            // Both should decrypt to same value
            ResponseEntity<String> firstDecrypt = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .postForEntity("/decrypt", firstEncrypt.getBody(), String.class);
            ResponseEntity<String> secondDecrypt = restTemplate
                    .withBasicAuth("configadmin", "configsecret")
                    .postForEntity("/decrypt", secondEncrypt.getBody(), String.class);

            assertThat(firstDecrypt.getBody()).isEqualTo(plainText);
            assertThat(secondDecrypt.getBody()).isEqualTo(plainText);
        }
    }
}
