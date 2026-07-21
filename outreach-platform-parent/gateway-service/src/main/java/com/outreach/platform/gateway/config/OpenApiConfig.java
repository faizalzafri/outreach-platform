package com.outreach.platform.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.1 configuration for the API Gateway Service.
 * Exposes /v3/api-docs in all environments and Swagger UI in non-production.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway")
                        .description("Spring Cloud Gateway - routing, rate limiting, JWT validation, CORS enforcement")
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
