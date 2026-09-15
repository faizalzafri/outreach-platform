package com.outreach.platform.report.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Permissive replacement for {@link SecurityConfig} in the {@code test} profile — that class is
 * excluded ({@code @Profile("!test")}) because its unconditional {@code oauth2ResourceServer(...)}
 * filter chain requires a {@code JwtDecoder} bean that isn't available once
 * {@code application-test.yml} excludes {@code OAuth2ResourceServerAutoConfiguration}.
 *
 * <p>Plain {@code @Configuration} (not {@code @TestConfiguration}) so component scanning picks
 * it up automatically for every {@code @SpringBootTest} with {@code @ActiveProfiles("test")} —
 * no per-test {@code @Import} needed.
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
}
