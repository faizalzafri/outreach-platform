package com.outreach.platform.notification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the Notification Service with OAuth2 JWT validation and stateless sessions.
 *
 * <p>Excluded from the {@code test} and {@code postgres-it} profiles: {@code @EnableMethodSecurity}
 * here enforces {@code @PreAuthorize} via AOP independently of the HTTP filter chain, so swapping
 * in {@code permissiveFilterChain} (app.security.enabled=false) was not enough to stop
 * role-gated controllers like {@code TemplateController} from 403ing every unauthenticated test
 * request — confirmed by {@code TemplateControllerIT} failing with exactly that until this guard
 * was added. See {@code TestSecurityConfig} in {@code src/test/java} for the permissive
 * replacement, which deliberately does not re-enable method security (mirrors event-service's
 * {@code SecurityConfig}/{@code TestSecurityConfig} split).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!test & !postgres-it")
public class SecurityConfig {

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
    public SecurityFilterChain permissiveFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );
        return http.build();
    }
}
