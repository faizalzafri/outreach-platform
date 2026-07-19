package com.outreach.platform.auth.config;

import com.outreach.platform.auth.service.LockoutAwareAuthenticationProvider;
import jakarta.inject.Inject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
     * Default security filter chain for form login.
     * Applied after the authorization server filter chain (Order 2).
     * Wires the lockout-aware authentication provider for brute-force protection.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http,
            LockoutAwareAuthenticationProvider authenticationProvider
    ) throws Exception {
        http
                .authenticationProvider(authenticationProvider)
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    /**
     * JDBC-backed user details service for authenticating resource owners.
     * Default admin user is provisioned on first startup if not already present.
     */
    @Bean
    public UserDetailsService userDetailsService(DataSource dataSource, PasswordEncoder passwordEncoder) {
        JdbcUserDetailsManager userManager = new JdbcUserDetailsManager(dataSource);

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
