package com.outreach.platform.auth;

import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.AccountStatus;
import com.outreach.platform.auth.repo.UserAccountRepository;
import com.outreach.platform.auth.service.AccountUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Authorities come from tenant memberships (seeded users hold one role each in the default tenant),
 * and platform admins carry only the platform role.
 */
@DisplayName("Role-Based Access Control")
class RoleBasedAccessControlTest extends BaseAuthIntegrationTest {

    @Inject
    private TestRestTemplate restTemplate;

    @Inject
    private AccountUserDetailsService userDetailsService;

    @Inject
    private UserAccountRepository accounts;

    @Inject
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Seeded users get the role of their tenant membership")
    void seededUsersGetTheirMembershipRole() {
        assertThat(authorities("admin")).containsExactly("ROLE_ADMIN");
        assertThat(authorities("priya_sharma")).containsExactly("ROLE_PMO");
        assertThat(authorities("anita_desai")).containsExactly("ROLE_POC");
    }

    @Test
    @DisplayName("A platform admin carries only ROLE_PLATFORM_ADMIN")
    void platformAdminCarriesOnlyThePlatformRole() {
        UserAccount account = accounts.findByUsername("rbac_platform_admin").orElseGet(() -> {
            UserAccount created = new UserAccount();
            created.setUsername("rbac_platform_admin");
            created.setPassword(passwordEncoder.encode("Platform@Admin123"));
            created.setStatus(AccountStatus.ACTIVE);
            created.setPlatformAdmin(true);
            return accounts.save(created);
        });

        assertThat(authorities(account.getUsername())).containsExactly("ROLE_PLATFORM_ADMIN");
    }

    @Test
    @DisplayName("An invited account cannot sign in until activated")
    void invitedAccountIsDisabled() {
        UserAccount invited = accounts.findByUsername("rbac_invited").orElseGet(() -> {
            UserAccount created = new UserAccount();
            created.setUsername("rbac_invited");
            created.setStatus(AccountStatus.INVITED);
            return accounts.save(created);
        });

        UserDetails details = userDetailsService.loadUserByUsername(invited.getUsername());
        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Client credentials tokens are issued with an issuer claim")
    void clientCredentialsTokenShouldBeValid() {
        MultiValueMap<String, String> tokenRequest = new LinkedMultiValueMap<>();
        tokenRequest.add("grant_type", "client_credentials");
        tokenRequest.add("scope", "openid");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth("outreach-services", "CHANGE_ME_IN_PRODUCTION");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/oauth2/token", new HttpEntity<>(tokenRequest, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String accessToken = response.getBody().get("access_token").toString();
        String payload = new String(Base64.getUrlDecoder().decode(accessToken.split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("\"iss\"");
    }

    private java.util.List<String> authorities(String username) {
        return userDetailsService.loadUserByUsername(username).getAuthorities().stream()
                .map(Object::toString)
                .toList();
    }
}
