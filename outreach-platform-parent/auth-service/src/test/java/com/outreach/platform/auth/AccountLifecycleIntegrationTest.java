package com.outreach.platform.auth;

import com.outreach.platform.auth.entity.OutboxMessage;
import com.outreach.platform.auth.repo.OutboxMessageRepository;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The full account lifecycle through real HTTP: an admin invites a user, the user activates from
 * the emailed link and signs in, then forgets and resets their password. Emails are asserted as
 * outbox events (the outbox publisher is off in tests).
 */
@DisplayName("Account lifecycle: invite, activate, sign in, reset")
class AccountLifecycleIntegrationTest extends BaseAuthIntegrationTest {

    private static final String DEFAULT_TENANT = "00000000-0000-0000-0000-000000000001";

    @Inject
    private OutboxMessageRepository outbox;

    @Test
    void invitedUserActivatesSignsInAndResetsTheirPassword() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "meera_" + suffix;
        String email = "meera." + suffix + "@example.com";
        String adminToken = new BrowserSession(baseUrl()).signIn("admin", "password");

        // 1. The admin invites a POC; no password is involved.
        HttpResponse<String> invited = new BrowserSession(baseUrl()).api("POST", "/api/auth/users", adminToken, """
                {"username":"%s","email":"%s","displayName":"Meera Iyer","role":"POC"}
                """.formatted(username, email));
        assertThat(invited.statusCode()).as(invited.body()).isEqualTo(201);
        assertThat(invited.body()).contains("\"status\":\"INVITED\"").doesNotContain("password");

        String activationLink = (String) latestPayload(RabbitMqConstants.ROUTING_KEY_IDENTITY_USER_INVITED, email)
                .get("activationLink");
        assertThat(activationLink).contains("/activate?token=");
        String activationPath = activationLink.substring(activationLink.indexOf("/activate"));

        // 2. The invited user cannot sign in yet.
        BrowserSession user = new BrowserSession(baseUrl());
        user.startAuthorization();
        assertThat(BrowserSession.location(user.login(username, "anything"))).contains("/login?error");

        // 3. Activation: a guessable password is refused with the reason, a good one activates.
        assertThat(user.get(activationPath).body()).contains(username).contains("Meera Iyer");
        HttpResponse<String> weak = user.submitForm(activationPath, "/activate", Map.of(
                "token", token(activationPath), "password", "Welcome@2026!!", "confirmPassword", "Welcome@2026!!"));
        assertThat(weak.body()).contains("too easy to guess");

        String firstPassword = "Lotus-Garden-47";
        HttpResponse<String> activated = user.submitForm(activationPath, "/activate", Map.of(
                "token", token(activationPath), "password", firstPassword, "confirmPassword", firstPassword));
        assertThat(BrowserSession.location(activated)).endsWith("/login?activated");
        assertThat(user.get("/login?activated").body()).contains("Your account is ready");

        // 4. The link is single-use.
        assertThat(user.get(activationPath).body()).contains("This link can");

        // 5. The new user signs in; the token names them and carries their tenant role.
        String userToken = new BrowserSession(baseUrl()).signIn(username, firstPassword);
        assertThat(BrowserSession.claims(userToken))
                .contains("\"name\":\"Meera Iyer\"")
                .contains("\"email\":\"" + email + "\"")
                .contains("\"realm_access\":{\"roles\":[\"ROLE_POC\"]}")
                .contains("\"tenant_id\":\"" + DEFAULT_TENANT + "\"");

        // 6. Forgot password: an unknown address gets the same answer and sends nothing.
        long resetsBefore = count(RabbitMqConstants.ROUTING_KEY_IDENTITY_PASSWORD_RESET_REQUESTED);
        BrowserSession anonymous = new BrowserSession(baseUrl());
        String unknown = anonymous.submitForm("/forgot-password", "/forgot-password",
                Map.of("identifier", "nobody-" + suffix + "@example.com")).body();
        String known = anonymous.submitForm("/forgot-password", "/forgot-password",
                Map.of("identifier", email.toUpperCase())).body();
        assertThat(unknown).contains("Check your email");
        assertThat(known).contains("Check your email");
        assertThat(count(RabbitMqConstants.ROUTING_KEY_IDENTITY_PASSWORD_RESET_REQUESTED)).isEqualTo(resetsBefore + 1);

        // 7. Reset: reusing the current password is refused; a new one works and the old one stops working.
        String resetLink = (String) latestPayload(RabbitMqConstants.ROUTING_KEY_IDENTITY_PASSWORD_RESET_REQUESTED, email)
                .get("resetLink");
        String resetPath = resetLink.substring(resetLink.indexOf("/reset-password"));
        HttpResponse<String> reused = anonymous.submitForm(resetPath, "/reset-password", Map.of(
                "token", token(resetPath), "password", firstPassword, "confirmPassword", firstPassword));
        assertThat(reused.body()).contains("used recently");

        String secondPassword = "Harbour-Lights-82";
        HttpResponse<String> reset = anonymous.submitForm(resetPath, "/reset-password", Map.of(
                "token", token(resetPath), "password", secondPassword, "confirmPassword", secondPassword));
        assertThat(BrowserSession.location(reset)).endsWith("/login?reset");
        assertThat(anonymous.get("/login?reset").body()).contains("Your password has been changed");
        assertThat(latestPayload(RabbitMqConstants.ROUTING_KEY_IDENTITY_PASSWORD_CHANGED, email)).isNotNull();

        assertThatThrownBy(() -> new BrowserSession(baseUrl()).signIn(username, firstPassword))
                .hasMessageContaining("/login?error");
        assertThat(new BrowserSession(baseUrl()).signIn(username, secondPassword)).isNotBlank();
    }

    @Test
    void userAdministrationIsLimitedToAdminsOfTheSameTenant() {
        BrowserSession browser = new BrowserSession(baseUrl());
        String adminToken = browser.signIn("admin", "password");
        String pmoToken = new BrowserSession(baseUrl()).signIn("priya_sharma", "password");

        assertThat(browser.api("GET", "/api/auth/users", adminToken, null).body())
                .contains("\"username\":\"priya_sharma\"");
        assertThat(browser.api("GET", "/api/auth/users", pmoToken, null).statusCode()).isEqualTo(403);
        assertThat(browser.api("GET", "/api/auth/users?tenantId=" + UUID.randomUUID(), adminToken, null).statusCode())
                .isEqualTo(403);
        // An admin cannot lock themselves out.
        assertThat(browser.api("POST", "/api/auth/users/a0000000-0000-0000-0000-000000000001/disable",
                adminToken, null).statusCode()).isEqualTo(400);
    }

    private Map<String, Object> latestPayload(String routingKey, String email) {
        List<OutboxMessage> messages = outbox.findByRoutingKeyOrderByCreatedDate(routingKey);
        return messages.reversed().stream()
                .map(OutboxMessage::getPayload)
                .filter(payload -> email.equalsIgnoreCase(String.valueOf(payload.get("email"))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No " + routingKey + " event for " + email));
    }

    private long count(String routingKey) {
        return outbox.findByRoutingKeyOrderByCreatedDate(routingKey).size();
    }

    private static String token(String pathWithToken) {
        return pathWithToken.substring(pathWithToken.indexOf("token=") + "token=".length());
    }
}
