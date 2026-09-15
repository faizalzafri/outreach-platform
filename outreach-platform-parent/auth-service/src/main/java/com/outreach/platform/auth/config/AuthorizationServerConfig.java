package com.outreach.platform.auth.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.outreach.platform.auth.entity.Tenant;
import com.outreach.platform.auth.entity.TenantMembership;
import com.outreach.platform.auth.service.TenantMembershipService;
import com.outreach.platform.auth.util.UserIdentifiers;
import jakarta.inject.Inject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Spring Authorization Server configuration.
 * Registers OAuth2 clients, configures JWKS endpoint, and enables token revocation (RFC 7009).
 *
 * <p>This configuration is only activated when {@code idp.provider=spring} is set.
 * When Keycloak is the active identity provider ({@code idp.provider=keycloak}),
 * these beans are not registered and the auth-service effectively becomes a no-op
 * (the application still starts for its Eureka registration but does not serve tokens).
 *
 * <p><strong>IdP Switching Mechanism:</strong> The platform supports two interchangeable identity
 * providers selected via the {@code idp.provider} property:
 * <ul>
 *   <li>{@code idp.provider=spring} — activates this embedded Spring Authorization Server</li>
 *   <li>{@code idp.provider=keycloak} — deactivates this config; Keycloak container serves tokens</li>
 * </ul>
 * Resource servers (gateway, business services) always validate tokens via the JWKS endpoint,
 * configured through {@code idp.jwks-uri}, regardless of which provider is active.
 */
@Configuration
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
@EnableConfigurationProperties(AuthServiceProperties.class)
public class AuthorizationServerConfig {

    private static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";
    private static final String NO_TENANT_ASSOCIATION_ERROR = "NO_TENANT_ASSOCIATION";

    private final AuthServiceProperties properties;
    private final TenantMembershipService tenantMembershipService;

    @Inject
    public AuthorizationServerConfig(AuthServiceProperties properties,
                                     TenantMembershipService tenantMembershipService) {
        this.properties = properties;
        this.tenantMembershipService = tenantMembershipService;
    }

    /**
     * Authorization server security filter chain.
     * Configures OIDC, token revocation, and JWKS endpoints.
     */
    @Bean
    @Order(1)
    @SuppressWarnings("removal")
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http
                // Redirect unauthenticated requests to login page
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                )
                // Accept access tokens for User Info and Client Registration endpoints
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    /**
     * PostgreSQL-backed registered client repository using Spring's JDBC implementation.
     * Clients are initialized on first startup if not already present.
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        JdbcRegisteredClientRepository repository = new JdbcRegisteredClientRepository(jdbcTemplate);

        // Register outreach-dashboard (public client, PKCE, Authorization Code)
        RegisteredClient dashboardClient = buildDashboardClient();
        if (repository.findByClientId(dashboardClient.getClientId()) == null) {
            repository.save(dashboardClient);
        }

        // Register outreach-services (confidential client, Client Credentials)
        RegisteredClient servicesClient = buildServicesClient();
        if (repository.findByClientId(servicesClient.getClientId()) == null) {
            repository.save(servicesClient);
        }

        return repository;
    }

    /**
     * JDBC-backed authorization service for storing active tokens and authorization codes.
     * Required for token revocation (RFC 7009) — the revocation endpoint looks up tokens here.
     *
     * <p>A custom ObjectMapper is configured with Spring Security and OAuth2 Authorization Server
     * Jackson modules to properly serialize/deserialize authorization attributes (fixes
     * ImmutableCollections deserialization issues).
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        JdbcOAuth2AuthorizationService authorizationService =
                new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);

        // Configure a custom ObjectMapper that can handle ImmutableCollections and other
        // Spring Security types stored in the authorization attributes column
        JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper rowMapper =
                new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(registeredClientRepository);

        ObjectMapper objectMapper = new ObjectMapper();
        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        List<Module> securityModules = SecurityJackson2Modules.getModules(classLoader);
        objectMapper.registerModules(securityModules);
        objectMapper.registerModule(new OAuth2AuthorizationServerJackson2Module());
        // Enable default typing to handle Java immutable collections (Map.of(), List.of())
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        rowMapper.setObjectMapper(objectMapper);
        authorizationService.setAuthorizationRowMapper(rowMapper);

        return authorizationService;
    }

    /**
     * JDBC-backed authorization consent service for persisting user consent decisions.
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository
    ) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClientRepository);
    }

    /**
     * Authorization server settings including issuer URI.
     */
    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer(properties.issuerUri())
                .build();
    }

    /**
     * JWK source for signing tokens. Uses RSA key pair.
     * In production, this should load from a persistent keystore.
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * JWT decoder for validating tokens at resource server endpoints within this service.
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     * Token customizer that enriches JWT access tokens with tenant claims.
     *
     * <p>Adds the following claims to access tokens:
     * <ul>
     *   <li>{@code tenant_id} — UUID of the user's active tenant</li>
     *   <li>{@code tenant_roles} — list of roles the user holds in the active tenant</li>
     *   <li>{@code platform_admin} — boolean flag for Platform_Admin users</li>
     *   <li>{@code realm_access.roles} — user authorities (for Keycloak compatibility)</li>
     * </ul>
     *
     * <p>For Platform_Admin users ({@code ROLE_PLATFORM_ADMIN}), the token includes
     * {@code platform_admin: true} and omits the {@code tenant_id} claim.
     *
     * <p>For regular users without any tenant membership, authentication is rejected
     * with error code {@code NO_TENANT_ASSOCIATION}.
     */
    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> {
            if (!OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                return;
            }

            // Skip tenant enrichment for client_credentials grants (service-to-service tokens)
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                return;
            }

            context.getClaims().claims(claims -> {
                var principal = context.getPrincipal();
                if (principal == null || principal.getAuthorities() == null) {
                    return;
                }

                // Add realm_access.roles for Keycloak format compatibility
                var roles = principal.getAuthorities().stream()
                        .map(Object::toString)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .toList();
                claims.put("realm_access", java.util.Map.of("roles", roles));

                // Check if user is a Platform_Admin
                boolean isPlatformAdmin = principal.getAuthorities().stream()
                        .anyMatch(auth -> PLATFORM_ADMIN_AUTHORITY.equals(auth.getAuthority()));

                claims.put("platform_admin", isPlatformAdmin);

                if (isPlatformAdmin) {
                    // Platform_Admin: set platform_admin claim, omit tenant_id
                    return;
                }

                // Regular user: resolve tenant membership
                String username = principal.getName();
                UUID userId = UserIdentifiers.fromUsername(username);

                Optional<Tenant> activeTenant = tenantMembershipService.getActiveTenantForUser(userId);

                if (activeTenant.isEmpty()) {
                    // User has no tenant membership → reject authentication
                    throw new OAuth2AuthenticationException(
                            new OAuth2Error(NO_TENANT_ASSOCIATION_ERROR,
                                    "User has no tenant membership", null));
                }

                // Add tenant_id claim
                claims.put("tenant_id", activeTenant.get().getId().toString());

                // Add tenant_roles claim (roles the user holds in the active tenant)
                List<TenantMembership> memberships = tenantMembershipService.findMembershipsByUserId(userId);
                UUID activeTenantId = activeTenant.get().getId();
                List<String> tenantRoles = memberships.stream()
                        .filter(m -> activeTenantId.equals(m.getTenantId()))
                        .map(m -> m.getRole().name())
                        .toList();
                claims.put("tenant_roles", tenantRoles);

                // Signals the frontend to redirect to the tenant-selection page instead of
                // trusting this (arbitrarily-picked) tenant_id — set only until the user makes
                // an explicit choice via POST /api/auth/select-tenant, never again after.
                claims.put("tenant_selection_required", tenantMembershipService.isTenantSelectionRequired(userId));
            });
        };
    }

    private RegisteredClient buildDashboardClient() {
        var clientsProps = properties.clients();
        var tokenProps = properties.token();

        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientsProps.dashboard().clientId())
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(clientsProps.dashboard().redirectUri())
                .postLogoutRedirectUri(clientsProps.dashboard().postLogoutRedirectUri())
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope("roles")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(tokenProps.accessTokenTimeToLive())
                        .refreshTokenTimeToLive(tokenProps.refreshTokenTimeToLive())
                        .reuseRefreshTokens(tokenProps.refreshTokenMaxReuse() > 0)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .build();
    }

    private RegisteredClient buildServicesClient() {
        var clientsProps = properties.clients();
        var tokenProps = properties.token();

        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientsProps.services().clientId())
                .clientSecret("{noop}" + clientsProps.services().clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope(OidcScopes.EMAIL)
                .scope("roles")
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(tokenProps.accessTokenTimeToLive())
                        .reuseRefreshTokens(false)
                        .build())
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .build())
                .build();
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }
}
