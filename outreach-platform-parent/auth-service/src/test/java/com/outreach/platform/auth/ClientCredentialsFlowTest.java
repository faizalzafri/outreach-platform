package com.outreach.platform.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import jakarta.inject.Inject;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the OAuth2 Client Credentials flow.
 * Validates Requirements 2.1, 2.5, 2.8.
 */
@DisplayName("Client Credentials Flow")
class ClientCredentialsFlowTest extends BaseAuthIntegrationTest {

    @Inject
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Should issue access token for valid client credentials")
    void shouldIssueAccessTokenForValidClientCredentials() {
        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "client_credentials");
        tokenRequest.add("scope", "openid profile email roles");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("outreach-services", "CHANGE_ME_IN_PRODUCTION");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(tokenRequest, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/oauth2/token", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKey("access_token");
        assertThat(body).containsKey("token_type");
        assertThat(body.get("token_type").toString()).isEqualToIgnoringCase("Bearer");

        // Verify access token is a valid JWT (3 parts)
        String accessToken = body.get("access_token").toString();
        assertThat(accessToken.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should include expected scopes in client credentials token")
    void shouldIncludeExpectedScopes() {
        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "client_credentials");
        tokenRequest.add("scope", "openid profile email roles");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("outreach-services", "CHANGE_ME_IN_PRODUCTION");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(tokenRequest, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/oauth2/token", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).containsKey("scope");
        String scopes = body.get("scope").toString();
        assertThat(scopes).contains("openid");
    }

    @Test
    @DisplayName("Should NOT return refresh token for client_credentials grant")
    void shouldNotReturnRefreshTokenForClientCredentials() {
        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "client_credentials");
        tokenRequest.add("scope", "openid");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("outreach-services", "CHANGE_ME_IN_PRODUCTION");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(tokenRequest, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/oauth2/token", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        // Client credentials should NOT include a refresh token
        assertThat(body).doesNotContainKey("refresh_token");
    }

    @Test
    @DisplayName("Should return 401 for invalid client secret")
    void shouldReturn401ForInvalidClientSecret() {
        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "client_credentials");
        tokenRequest.add("scope", "openid");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("outreach-services", "wrong-secret");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(tokenRequest, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/oauth2/token", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Should return 401 for unknown client ID")
    void shouldReturn401ForUnknownClientId() {
        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "client_credentials");
        tokenRequest.add("scope", "openid");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("nonexistent-client", "some-secret");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(tokenRequest, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/oauth2/token", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
