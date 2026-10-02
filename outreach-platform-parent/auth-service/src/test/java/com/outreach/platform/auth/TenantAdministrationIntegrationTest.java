package com.outreach.platform.auth;

import com.outreach.platform.auth.entity.OutboxMessage;
import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.AccountStatus;
import com.outreach.platform.auth.repo.OutboxMessageRepository;
import com.outreach.platform.auth.repo.UserAccountRepository;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.http.HttpResponse;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A platform admin creates an organization from scratch, hands it to its first admin, and suspends it. */
@DisplayName("Tenant administration by a platform admin")
class TenantAdministrationIntegrationTest extends BaseAuthIntegrationTest {

    @Inject
    private UserAccountRepository accounts;

    @Inject
    private PasswordEncoder encoder;

    @Inject
    private OutboxMessageRepository outbox;

    @Test
    void createOrganization_inviteItsAdmin_thenSuspendIt() {
        String suffix = Long.toString(System.nanoTime(), 36);
        String platformToken = new BrowserSession(baseUrl()).signIn(platformAdmin(), "Platform-Keeper-91");
        assertThat(BrowserSession.claims(platformToken)).contains("\"platform_admin\":true")
                .contains("\"roles\":[\"ROLE_PLATFORM_ADMIN\"]");
        BrowserSession api = new BrowserSession(baseUrl());

        // Create the organization; its first admin is invited by email.
        HttpResponse<String> created = api.api("POST", "/api/tenants", platformToken, """
                {"name":"Acme Volunteers %1$s","slug":"acme-%1$s","adminDisplayName":"Asha Menon",
                 "adminUsername":"asha_%1$s","adminEmail":"asha.%1$s@acme.example"}""".formatted(suffix));
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        String tenantId = field(created.body(), "id");
        assertThat(created.body()).contains("\"role\":\"ADMIN\"").contains("\"status\":\"INVITED\"");

        assertThat(api.api("POST", "/api/tenants", platformToken, """
                {"name":"Another %1$s","slug":"acme-%1$s","adminDisplayName":"X","adminUsername":"x_%1$s",
                 "adminEmail":"x.%1$s@acme.example"}""".formatted(suffix)).statusCode()).isEqualTo(409);

        // The first admin activates and lands in the new organization.
        String link = (String) invitationFor("asha." + suffix).get("activationLink");
        String path = link.substring(link.indexOf("/activate"));
        new BrowserSession(baseUrl()).submitForm(path, "/activate", Map.of(
                "token", path.substring(path.indexOf("token=") + 6),
                "password", "Monsoon-Garden-38", "confirmPassword", "Monsoon-Garden-38"));
        String adminToken = new BrowserSession(baseUrl()).signIn("asha_" + suffix, "Monsoon-Garden-38");
        assertThat(BrowserSession.claims(adminToken)).contains("\"tenant_id\":\"" + tenantId + "\"")
                .contains("\"roles\":[\"ROLE_ADMIN\"]");
        assertThat(api.api("GET", "/api/tenants/current", adminToken, null).body())
                .contains("Acme Volunteers " + suffix);
        assertThat(api.api("GET", "/api/tenants", adminToken, null).statusCode()).isEqualTo(403);

        // The platform admin sees the organization's people and can manage them.
        assertThat(api.api("GET", "/api/auth/users?tenantId=" + tenantId, platformToken, null).body())
                .contains("\"username\":\"asha_" + suffix + "\"");
        assertThat(api.api("GET", "/api/tenants?search=acme-" + suffix, platformToken, null).body())
                .contains("\"memberCount\":1");

        // Suspension: the gateway's status lookup says so and members can no longer get tokens.
        assertThat(api.api("POST", "/api/tenants/" + tenantId + "/suspend", platformToken, null).statusCode())
                .isEqualTo(200);
        assertThat(api.get("/internal/tenants/" + tenantId + "/status").body()).contains("SUSPENDED");
        assertThatThrownBy(() -> new BrowserSession(baseUrl()).signIn("asha_" + suffix, "Monsoon-Garden-38"))
                .isInstanceOf(AssertionError.class);

        api.api("POST", "/api/tenants/" + tenantId + "/activate", platformToken, null);
        assertThat(api.get("/internal/tenants/" + tenantId + "/status").body()).contains("ACTIVE");
        assertThat(new BrowserSession(baseUrl()).signIn("asha_" + suffix, "Monsoon-Garden-38")).isNotBlank();
    }

    private String platformAdmin() {
        String username = "pa_tenants";
        if (accounts.findByUsername(username).isEmpty()) {
            UserAccount account = new UserAccount();
            account.setUsername(username);
            account.setDisplayName("Platform Admin");
            account.setEmail("pa.tenants@platform.example");
            account.setPassword(encoder.encode("Platform-Keeper-91"));
            account.setStatus(AccountStatus.ACTIVE);
            account.setPlatformAdmin(true);
            accounts.save(account);
        }
        return username;
    }

    private Map<String, Object> invitationFor(String emailPrefix) {
        return outbox.findByRoutingKeyOrderByCreatedDate(RabbitMqConstants.ROUTING_KEY_IDENTITY_USER_INVITED)
                .reversed().stream()
                .map(OutboxMessage::getPayload)
                .filter(p -> String.valueOf(p.get("email")).startsWith(emailPrefix))
                .findFirst()
                .orElseThrow();
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\":\"([^\"]+)\"").matcher(json);
        assertThat(m.find()).as(json).isTrue();
        return m.group(1);
    }
}
