package com.outreach.platform.event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permissive replacement for {@link com.outreach.platform.event.config.SecurityConfig} in the
 * {@code test} profile — that class is excluded ({@code @Profile("!test")}) because its
 * unconditional {@code oauth2ResourceServer(...)} filter chain requires a {@code JwtDecoder}
 * bean that isn't available once {@code application-test.yml} excludes
 * {@code OAuth2ResourceServerAutoConfiguration}.
 *
 * <p>Plain {@code @Configuration} (not {@code @TestConfiguration}) so component scanning picks
 * it up automatically for every {@code @SpringBootTest} with {@code @ActiveProfiles("test")} —
 * no per-test {@code @Import} needed.
 *
 * <p>Permits every request. None of this service's integration tests send an Authorization
 * header at all; they test tenant isolation and business logic via {@code X-Tenant-ID} and
 * {@code TenantContext} instead, which is unrelated to authentication. If a future test needs to
 * assert real RBAC/JWT behavior, that belongs in a narrower, security-specific test with its own
 * configuration — not a reason to make every other integration test carry that weight.
 */
@Configuration
@EnableWebSecurity
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
