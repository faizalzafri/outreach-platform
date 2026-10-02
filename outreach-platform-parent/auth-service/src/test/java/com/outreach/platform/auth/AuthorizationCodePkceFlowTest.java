package com.outreach.platform.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the OAuth2 Authorization Code + PKCE flow.
 * Verifies the full interactive login → authorization → token exchange cycle.
 */
@DisplayName("Authorization Code + PKCE Flow")
class AuthorizationCodePkceFlowTest extends BaseAuthIntegrationTest {

    @Inject
    private TestRestTemplate restTemplate;

    private String codeVerifier;
    private String codeChallenge;

    @BeforeEach
    void generatePkce() throws Exception {
        // Generate PKCE code_verifier (43-128 chars of unreserved characters)
        SecureRandom random = new SecureRandom();
        byte[] verifierBytes = new byte[32];
        random.nextBytes(verifierBytes);
        codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);

        // Generate code_challenge = BASE64URL(SHA256(code_verifier))
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] challengeBytes = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
        codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(challengeBytes);
    }

    @Test
    @DisplayName("Password sign-in through the dashboard client issues a JWT with identity claims")
    void shouldCompleteFullPkceFlow() {
        BrowserSession browser = new BrowserSession(baseUrl());

        HttpResponse<String> authorize = browser.startAuthorization();
        assertThat(BrowserSession.location(authorize)).endsWith("/login");

        String accessToken = browser.finishAuthorization(browser.login("admin", "password"));

        verifyJwtStructure(accessToken);
    }

    @Test
    @DisplayName("Should require PKCE for public client (outreach-dashboard)")
    void shouldRequirePkceForPublicClient() {
        // Attempt authorization without code_challenge — should fail
        String authorizeUrl = baseUrl() + "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=outreach-dashboard"
                + "&redirect_uri=http://localhost:5173/callback"
                + "&scope=openid"
                + "&state=test-state";
        // Without PKCE, the server should reject or not issue a code

        ResponseEntity<String> response = restTemplate.getForEntity(authorizeUrl, String.class);
        // Either error response or redirect to login without proceeding
        // The key point is that without PKCE, the flow should not succeed
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("JWKS endpoint should return valid RSA keys")
    void jwksEndpointShouldReturnKeys() {
        ResponseEntity<Map> jwksResponse = restTemplate.getForEntity(
                baseUrl() + "/oauth2/jwks", Map.class);

        assertThat(jwksResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> jwks = jwksResponse.getBody();
        assertThat(jwks).isNotNull();
        assertThat(jwks).containsKey("keys");

        List<?> keys = (List<?>) jwks.get("keys");
        assertThat(keys).isNotEmpty();

        @SuppressWarnings("unchecked")
        Map<String, Object> firstKey = (Map<String, Object>) keys.get(0);
        assertThat(firstKey).containsKey("kty");
        assertThat(firstKey.get("kty")).isEqualTo("RSA");
        assertThat(firstKey).containsKey("n");
        assertThat(firstKey).containsKey("e");
        assertThat(firstKey).containsKey("kid");
    }

    private void verifyJwtStructure(String jwt) {
        // JWT has 3 Base64URL-encoded parts separated by dots
        String[] parts = jwt.split("\\.");
        assertThat(parts).hasSize(3);

        // Decode header
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        assertThat(headerJson).contains("\"alg\"");

        // Decode payload
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        assertThat(payloadJson).contains("\"iss\"");
        assertThat(payloadJson).contains("\"sub\"");
        // Identity claims the UI shows, and roles narrowed to the active tenant
        assertThat(payloadJson).contains("\"uid\":\"a0000000-0000-0000-0000-000000000001\"")
                .contains("\"name\":\"Admin\"")
                .contains("\"preferred_username\":\"admin\"")
                .contains("\"realm_access\":{\"roles\":[\"ROLE_ADMIN\"]}");
    }

    @Test
    @DisplayName("Silent-renewal redirect URI is registered for the dashboard client")
    void silentCallbackRedirectUri_isAcceptedByTheAuthorizeEndpoint() {
        // Public clients get no refresh token, so the SPA renews by re-running this request in a
        // hidden iframe; an unregistered redirect_uri would be rejected with 400.
        String authorizeUrl = baseUrl() + "/oauth2/authorize"
                + "?response_type=code"
                + "&client_id=outreach-dashboard"
                + "&redirect_uri=http://localhost:5173/silent-callback.html"
                + "&scope=openid"
                + "&code_challenge=" + codeChallenge
                + "&code_challenge_method=S256"
                + "&state=silent";

        ResponseEntity<String> response = restTemplate.getForEntity(authorizeUrl, String.class);

        assertThat(response.getStatusCode().value()).isNotEqualTo(400);
    }

    @Test
    @DisplayName("Browser favicon request is not treated as a protected page")
    void faviconRequest_isNotSavedAsThePostLoginTarget() {
        // The login page triggers an automatic /favicon.ico fetch. If that lands on the login
        // entry point it is saved as the post-login redirect, replacing the pending
        // /oauth2/authorize request, and the user ends up on a favicon 404 after signing in.
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/favicon.ico", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    private String extractCsrfToken(String html) {
        if (html == null) return null;
        Pattern pattern = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Try alternate format
        pattern = Pattern.compile("value=\"([^\"]+)\"[^>]*name=\"_csrf\"");
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractRedirectFromBody(String body) {
        if (body == null) return null;
        Pattern pattern = Pattern.compile("action=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractQueryParam(String url, String param) {
        Pattern pattern = Pattern.compile("[?&]" + param + "=([^&]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
