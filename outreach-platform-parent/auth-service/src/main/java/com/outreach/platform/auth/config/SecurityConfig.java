package com.outreach.platform.auth.config;

import com.outreach.platform.auth.service.AccountLockoutService;
import com.outreach.platform.auth.service.LockoutAwareAuthenticationProvider;
import jakarta.inject.Inject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

/**
 * Default security filter chain for form-login and resource protection.
 * Handles user authentication for the Authorization Code flow (interactive login).
 *
 * <p>Only active when {@code idp.provider=spring}. When Keycloak is the active provider,
 * this configuration is not loaded and the auth-service does not expose login endpoints.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class SecurityConfig {

    private final AuthServiceProperties properties;

    @Inject
    public SecurityConfig(AuthServiceProperties properties) {
        this.properties = properties;
    }

    /**
     * Bearer-token security filter chain for the tenant-selection API, scoped to {@code /api/auth/**}
     * only. The SPA calls this with an already-issued access token (not a browser session), so it
     * needs JWT resource-server validation rather than the form-login flow the default chain below
     * uses — without this, a Bearer-token request here would fall through to that chain's
     * {@code anyRequest().authenticated()} and fail against session-based auth instead.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiAuthSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/auth/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * Default security filter chain for form login.
     * Applied after the authorization server filter chain (Order 1) and the API auth chain
     * (Order 2). Wires the lockout-aware authentication provider for brute-force protection.
     *
     * <p>The provider is created inline (not as a separate @Bean) to prevent
     * Spring Boot's auto-configuration from also registering it in the global
     * AuthenticationManager, which would cause each login attempt to be processed
     * twice and double-count failed attempts.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            AccountLockoutService lockoutService
    ) throws Exception {
        LockoutAwareAuthenticationProvider authenticationProvider =
                new LockoutAwareAuthenticationProvider(userDetailsService, passwordEncoder, lockoutService);

        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/.well-known/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .permitAll()
                )
                .requestCache(cache -> cache
                        .requestCache(new org.springframework.security.web.savedrequest.HttpSessionRequestCache() {{
                            setMatchingRequestParameterName(null);
                            setRequestMatcher(request -> {
                                String uri = request.getRequestURI();
                                // Don't cache Chrome DevTools or .well-known requests
                                return !uri.startsWith("/.well-known") && !uri.contains("appspecific");
                            });
                        }})
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/logout", "GET"))
                        .logoutSuccessUrl("http://localhost:5173/login")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }

    /**
     * JDBC-backed user details service for authenticating resource owners.
     * Uses auth_users/auth_authorities tables to avoid conflict with the platform users table.
     * Default admin user is provisioned on first startup if not already present.
     */
    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource, PasswordEncoder passwordEncoder) {
        JdbcUserDetailsManager userManager = new JdbcUserDetailsManager(dataSource);

        // Point to auth-service specific tables (avoids conflict with event-service users table)
        userManager.setUsersByUsernameQuery(
                "SELECT username, password, enabled FROM auth_users WHERE username = ?");
        userManager.setAuthoritiesByUsernameQuery(
                "SELECT username, authority FROM auth_authorities WHERE username = ?");
        userManager.setCreateUserSql(
                "INSERT INTO auth_users (username, password, enabled) VALUES (?,?,?)");
        userManager.setCreateAuthoritySql(
                "INSERT INTO auth_authorities (username, authority) VALUES (?,?)");
        userManager.setUserExistsSql(
                "SELECT username FROM auth_users WHERE username = ?");
        userManager.setDeleteUserSql(
                "DELETE FROM auth_users WHERE username = ?");
        userManager.setDeleteUserAuthoritiesSql(
                "DELETE FROM auth_authorities WHERE username = ?");
        userManager.setUpdateUserSql(
                "UPDATE auth_users SET password = ?, enabled = ? WHERE username = ?");
        userManager.setChangePasswordSql(
                "UPDATE auth_users SET password = ? WHERE username = ?");

        // Provision default admin if not present
        if (!userManager.userExists("admin")) {
            UserDetails admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@12345!"))
                    .roles("ADMIN")
                    .build();
            userManager.createUser(admin);
        }

        return userManager;
    }

    /**
     * Delegating password encoder supporting multiple formats ({noop}, {bcrypt}, etc.).
     * Required for compatibility with OAuth2 client secrets stored with {noop} prefix
     * and user passwords stored with {bcrypt} prefix.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
