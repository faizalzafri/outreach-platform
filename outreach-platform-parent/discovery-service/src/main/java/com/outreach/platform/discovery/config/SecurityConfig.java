package com.outreach.platform.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration that permits unauthenticated Eureka client endpoints while protecting the dashboard with HTTP Basic.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for Eureka client REST calls (stateless service-to-service communication)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Eureka client endpoints must be accessible without credentials
                .requestMatchers("/eureka/**").permitAll()
                // Actuator health endpoint for load balancers and probes
                .requestMatchers("/actuator/health").permitAll()
                // Everything else (dashboard, actuator details) requires authentication
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
