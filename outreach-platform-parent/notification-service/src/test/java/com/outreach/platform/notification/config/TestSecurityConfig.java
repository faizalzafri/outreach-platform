package com.outreach.platform.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permissive replacement for {@link com.outreach.platform.notification.config.SecurityConfig} in
 * the {@code test}/{@code postgres-it} profiles — that class is excluded there
 * ({@code @Profile("!test & !postgres-it")}) because its {@code @EnableMethodSecurity} enforces
 * {@code @PreAuthorize} via AOP independently of the HTTP filter chain, so a permissive filter
 * chain alone (the old {@code app.security.enabled=false} toggle) wasn't enough to stop
 * role-gated controllers from 403ing every unauthenticated test request.
 *
 * <p>Plain {@code @Configuration} (not {@code @TestConfiguration}) so component scanning picks
 * it up automatically for every {@code @SpringBootTest} on these profiles — no per-test
 * {@code @Import} needed. (The previous version of this class WAS {@code @TestConfiguration} and
 * was never imported anywhere, so it silently did nothing — confirmed by grepping the whole test
 * tree for a reference to it.)
 */
@Configuration
@EnableWebSecurity
@Profile("test | postgres-it")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
