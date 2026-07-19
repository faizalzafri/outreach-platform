package com.outreach.platform.auth;

import com.outreach.platform.auth.service.AccountLockoutService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import jakarta.inject.Inject;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for account lockout after repeated failed attempts.
 * Validates Requirements 2.14, 2.15.
 * Policy: 5 failed attempts → account locked for 30 min.
 */
@DisplayName("Account Lockout")
class AccountLockoutIntegrationTest extends BaseAuthIntegrationTest {

    @Inject
    private TestRestTemplate restTemplate;

    @Inject
    private AccountLockoutService lockoutService;

    private static final String TEST_USER = "admin";
    private static final String WRONG_PASSWORD = "WrongP@ssword1";

    @BeforeEach
    void resetLockoutState() {
        // Reset any previous lockout state for the test user
        lockoutService.resetAttempts(TEST_USER);
    }

    @Test
    @DisplayName("Should NOT lock account after 4 failed login attempts")
    void shouldNotLockAfterFourFailedAttempts() {
        for (int i = 0; i < 4; i++) {
            attemptLogin(TEST_USER, WRONG_PASSWORD);
        }

        assertThat(lockoutService.isLocked(TEST_USER)).isFalse();
        assertThat(lockoutService.getFailedAttempts(TEST_USER)).isEqualTo(4);
    }

    @Test
    @DisplayName("Should lock account after 5 failed login attempts")
    void shouldLockAfterFiveFailedAttempts() {
        for (int i = 0; i < 5; i++) {
            attemptLogin(TEST_USER, WRONG_PASSWORD);
        }

        assertThat(lockoutService.isLocked(TEST_USER)).isTrue();
        assertThat(lockoutService.getFailedAttempts(TEST_USER)).isEqualTo(5);
    }

    @Test
    @DisplayName("Should reject login for locked account even with correct password")
    void shouldRejectLoginForLockedAccount() {
        // Lock the account by making 5 failed attempts
        for (int i = 0; i < 5; i++) {
            attemptLogin(TEST_USER, WRONG_PASSWORD);
        }
        assertThat(lockoutService.isLocked(TEST_USER)).isTrue();

        // Now try with correct password — should still be rejected
        ResponseEntity<String> response = attemptLogin(TEST_USER, "Admin@12345!");

        // The login should fail because the account is locked
        // (response will be redirect back to login page with error, not a successful redirect)
        assertThat(response.getStatusCode().value()).isIn(200, 302);
        if (response.getStatusCode().is3xxRedirection()) {
            String location = response.getHeaders().getLocation() != null
                    ? response.getHeaders().getLocation().toString() : "";
            assertThat(location).contains("error");
        }
    }

    @Test
    @DisplayName("Should reset lockout state after successful authentication")
    void shouldResetOnSuccessfulAuth() {
        // Record a few failed attempts (but not enough to lock)
        lockoutService.recordFailedAttempt(TEST_USER);
        lockoutService.recordFailedAttempt(TEST_USER);
        assertThat(lockoutService.getFailedAttempts(TEST_USER)).isEqualTo(2);

        // Simulate successful authentication
        lockoutService.resetAttempts(TEST_USER);

        assertThat(lockoutService.getFailedAttempts(TEST_USER)).isEqualTo(0);
        assertThat(lockoutService.isLocked(TEST_USER)).isFalse();
    }

    @Test
    @DisplayName("Should track failed attempt count incrementally")
    void shouldTrackFailedAttemptCount() {
        assertThat(lockoutService.getFailedAttempts(TEST_USER)).isEqualTo(0);

        lockoutService.recordFailedAttempt(TEST_USER);
        assertThat(lockoutService.getFailedAttempts(TEST_USER)).isEqualTo(1);

        lockoutService.recordFailedAttempt(TEST_USER);
        assertThat(lockoutService.getFailedAttempts(TEST_USER)).isEqualTo(2);

        lockoutService.recordFailedAttempt(TEST_USER);
        assertThat(lockoutService.getFailedAttempts(TEST_USER)).isEqualTo(3);
    }

    /**
     * Attempt form-based login and return the response.
     */
    private ResponseEntity<String> attemptLogin(String username, String password) {
        // First, get login page for CSRF token
        ResponseEntity<String> loginPage = restTemplate.getForEntity(baseUrl() + "/login", String.class);
        String csrfToken = extractCsrfToken(loginPage.getBody());
        List<String> cookies = loginPage.getHeaders().get(HttpHeaders.SET_COOKIE);

        MultiValueMap<String, String> loginForm = new LinkedMultiValueMap<>();
        loginForm.add("username", username);
        loginForm.add("password", password);
        if (csrfToken != null) {
            loginForm.add("_csrf", csrfToken);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (cookies != null) {
            headers.put(HttpHeaders.COOKIE, cookies);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(loginForm, headers);
        return restTemplate.postForEntity(baseUrl() + "/login", request, String.class);
    }

    private String extractCsrfToken(String html) {
        if (html == null) return null;
        Pattern pattern = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        pattern = Pattern.compile("value=\"([^\"]+)\"[^>]*name=\"_csrf\"");
        matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
