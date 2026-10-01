package com.outreach.platform.auth;

import com.outreach.platform.auth.entity.OutboxMessage;
import com.outreach.platform.auth.repo.OutboxMessageRepository;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One-time codes as a tenant admin configures them: a second sign-in step, a limited number of
 * attempts, and a code for password changes. Codes are read from the outbox (publishing is off).
 */
@DisplayName("One-time codes: policy, sign-in step, password change")
class OtpIntegrationTest extends BaseAuthIntegrationTest {

    private static final String POLICY_OFF = policy(false, false);

    @Inject
    private OutboxMessageRepository outbox;

    @Inject
    private com.outreach.platform.auth.repo.UserAccountRepository accounts;

    /** The test database has no event-service table to copy seed emails from, so give two accounts one. */
    @org.junit.jupiter.api.BeforeEach
    void seedEmails() {
        Map.of("admin", "admin@outreach-platform.com", "priya_sharma", "priya.sharma@outreach-platform.com")
                .forEach((username, email) -> accounts.findByUsername(username).ifPresent(account -> {
                    account.setEmail(email);
                    accounts.save(account);
                }));
    }

    private String adminToken() {
        return new BrowserSession(baseUrl()).signIn("admin", "password");
    }

    @AfterEach
    void turnCodesOffAgain() {
        // The database is shared with the other test classes, which sign in without codes.
        BrowserSession browser = new BrowserSession(baseUrl());
        String token = browser.signInWithAnyCode("admin", "password", username -> latestCode(username.split("_")[0]));
        browser.api("PUT", "/api/auth/security-policy", token, POLICY_OFF);
    }

    @Test
    void signInNeedsTheEmailedCode_whenTheTenantTurnsItOn() {
        HttpResponse<String> saved = new BrowserSession(baseUrl())
                .api("PUT", "/api/auth/security-policy", adminToken(), policy(true, false));
        assertThat(saved.statusCode()).as(saved.body()).isEqualTo(200);
        assertThat(saved.body()).contains("\"LOGIN\":{\"enabled\":true");

        BrowserSession browser = new BrowserSession(baseUrl());
        browser.startAuthorization();
        HttpResponse<String> afterPassword = browser.login("priya_sharma", "password");
        assertThat(BrowserSession.location(afterPassword)).endsWith("/login/otp");
        assertThat(browser.get("/login/otp").body()).contains("p•••@");

        HttpResponse<String> wrong = browser.submitForm("/login/otp", "/login/otp", Map.of("code", "000000"));
        assertThat(wrong.body()).contains("attempts left");

        HttpResponse<String> right = browser.submitForm("/login/otp", "/login/otp",
                Map.of("code", latestCode("priya")));
        String token = browser.finishAuthorization(right);
        assertThat(BrowserSession.claims(token)).contains("\"preferred_username\":\"priya_sharma\"");
    }

    @Test
    void aCodeDiesAfterItsAttempts_andSignInStartsOver() {
        new BrowserSession(baseUrl()).api("PUT", "/api/auth/security-policy", adminToken(), policy(true, false));

        BrowserSession browser = new BrowserSession(baseUrl());
        browser.startAuthorization();
        browser.login("priya_sharma", "password");
        String code = latestCode("priya");

        HttpResponse<String> last = null;
        for (int i = 0; i < 3; i++) {
            last = browser.submitForm("/login/otp", "/login/otp", Map.of("code", "999999"));
        }
        assertThat(BrowserSession.location(last)).endsWith("/login?otpExpired");
        // The real code no longer works either: the parked sign-in is gone.
        assertThat(BrowserSession.location(browser.submitForm("/login", "/login/otp", Map.of("code", code))))
                .endsWith("/login");
    }

    @Test
    void passwordChangeAsksForACode_thenAcceptsIt() {
        String suffix = Long.toString(System.nanoTime(), 36);
        String adminToken = adminToken();
        BrowserSession admin = new BrowserSession(baseUrl());
        admin.api("PUT", "/api/auth/security-policy", adminToken, policy(false, true));

        // A fresh user, so the password change doesn't disturb the shared seed accounts.
        admin.api("POST", "/api/auth/users", adminToken, """
                {"username":"otp_%s","email":"otp.%s@example.com","displayName":"Otp User","role":"POC"}
                """.formatted(suffix, suffix));
        String link = (String) latest(RabbitMqConstants.ROUTING_KEY_IDENTITY_USER_INVITED, "otp." + suffix)
                .get("activationLink");
        String path = link.substring(link.indexOf("/activate"));
        BrowserSession user = new BrowserSession(baseUrl());
        user.submitForm(path, "/activate", Map.of("token", path.substring(path.indexOf("token=") + 6),
                "password", "Lotus-Garden-47", "confirmPassword", "Lotus-Garden-47"));
        String userToken = new BrowserSession(baseUrl()).signIn("otp_" + suffix, "Lotus-Garden-47");

        String change = """
                {"currentPassword":"Lotus-Garden-47","newPassword":"Harbour-Lights-82"%s}""";
        HttpResponse<String> first = user.api("POST", "/api/auth/me/password", userToken, change.formatted(""));
        assertThat(first.statusCode()).isEqualTo(400);
        assertThat(first.body()).contains("\"error\":\"OTP_REQUIRED\"");

        HttpResponse<String> wrong = user.api("POST", "/api/auth/me/password", userToken,
                change.formatted(",\"otpCode\":\"000000\""));
        assertThat(wrong.body()).contains("otpCode");

        HttpResponse<String> done = user.api("POST", "/api/auth/me/password", userToken,
                change.formatted(",\"otpCode\":\"" + latestCode("otp." + suffix) + "\""));
        assertThat(done.statusCode()).as(done.body()).isEqualTo(204);
        assertThat(new BrowserSession(baseUrl()).signIn("otp_" + suffix, "Harbour-Lights-82")).isNotBlank();
    }

    @Test
    void aTenantCannotLoosenThePlatformPasswordRules() {
        HttpResponse<String> response = new BrowserSession(baseUrl()).api("PUT", "/api/auth/security-policy",
                adminToken(), POLICY_OFF.replace("\"passwordMinLength\":12", "\"passwordMinLength\":10"));
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("platform minimum");
    }

    private String latestCode(String emailPrefix) {
        return (String) latest(RabbitMqConstants.ROUTING_KEY_IDENTITY_OTP_ISSUED, emailPrefix).get("code");
    }

    private Map<String, Object> latest(String routingKey, String emailPrefix) {
        return outbox.findByRoutingKeyOrderByCreatedDate(routingKey).reversed().stream()
                .map(OutboxMessage::getPayload)
                .filter(p -> String.valueOf(p.get("email")).startsWith(emailPrefix))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + routingKey + " for " + emailPrefix));
    }

    private static String policy(boolean login, boolean passwordChange) {
        String otp = """
                {"enabled":%s,"length":6,"ttlSeconds":300,"maxAttempts":3,"resendCooldownSeconds":0}""";
        return """
                {"passwordMinLength":12,"passwordHistoryCount":5,"otp":{"LOGIN":%s,"PASSWORD_RESET":%s,"PASSWORD_CHANGE":%s}}
                """.formatted(otp.formatted(login), otp.formatted(false), otp.formatted(passwordChange));
    }
}
