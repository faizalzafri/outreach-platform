package com.outreach.platform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Embedded Spring Authorization Server providing OAuth 2.1 / OIDC identity services.
 * Activated via Spring profile ({@code idp.provider=spring}) as a lightweight alternative
 * to the Keycloak deployment. Mirrors the Keycloak realm configuration including clients,
 * roles, password policy, and token settings.
 *
 * <p>When {@code idp.provider=keycloak}, the authorization server beans are not registered
 * and this application only serves as a health-check/discovery participant.
 */
@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
