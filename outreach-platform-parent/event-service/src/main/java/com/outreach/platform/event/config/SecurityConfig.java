package com.outreach.platform.event.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

/**
 * OAuth2 Resource Server security configuration for the Event Service.
 *
 * <p>Validates incoming JWTs using the JWKS endpoint configured via
 * {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri}.
 * Actuator health/info endpoints are publicly accessible for container probes;
 * all other endpoints require authentication.</p>
 *
 * <p>{@code @EnableMethodSecurity} enables {@code @PreAuthorize} annotations
 * for fine-grained role-based access control on controller methods.</p>
 *
 * <p>Excluded from the {@code test} profile: {@code application-test.yml} already excludes
 * Spring Boot's own {@code OAuth2ResourceServerAutoConfiguration} intending to disable JWT
 * validation for integration tests, but that exclusion doesn't reach this class's own
 * unconditional {@code oauth2ResourceServer(...)} filter chain, which requires a
 * {@code JwtDecoder} bean regardless. Without this profile guard, every {@code @SpringBootTest}
 * with a web environment fails to even start ({@code NoSuchBeanDefinitionException} for
 * {@code JwtDecoder}) — see {@code com.outreach.platform.event.config.TestSecurityConfig} in
 * {@code src/test/java} for the permissive replacement used instead.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!test")
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
