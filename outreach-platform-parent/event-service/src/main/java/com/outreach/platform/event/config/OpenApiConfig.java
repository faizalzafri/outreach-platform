package com.outreach.platform.event.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.1 configuration for the Event Service.
 * Exposes /v3/api-docs in all environments and Swagger UI in non-production.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eventServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Event Service API")
                        .description("Event lifecycle management - CRUD, state machine, POC assignments, volunteers, beneficiaries, and admin operations")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Outreach Platform Team")
                                .email("platform@outreach.com")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .schemaRequirement("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token obtained from the Identity Provider (Keycloak or Spring Authorization Server)"));
    }
}
