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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import com.outreach.platform.common.config.RealmRoleJwtAuthenticationConverter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import com.outreach.platform.auth.service.otp.OtpLoginSuccessHandler;


/**
 * Default security filter chain for form-login and resource protection.
 * Handles user authentication for the Authorization Code flow (interactive login).
 *
 * <p>Only active when {@code idp.provider=spring}. When Keycloak is the active provider,
 * this configuration is not loaded and the auth-service does not expose login endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                // Roles live in realm_access.roles (as on every other service), for @PreAuthorize
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                        .jwtAuthenticationConverter(new RealmRoleJwtAuthenticationConverter())));

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
            AccountLockoutService lockoutService,
            RequestCache requestCache,
            OtpLoginSuccessHandler otpLoginSuccessHandler
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
                        // The browser fetches this automatically on the login page. If it reached
                        // the login entry point it would be saved as the post-login redirect
                        // (the custom request cache below has no favicon exclusion), replacing the
                        // pending /oauth2/authorize request.
                        .requestMatchers("/favicon.ico").permitAll()
                        // Account pages reached from emailed links or the sign-in page; the
                        // single-use token in the link is the credential
                        .requestMatchers("/activate", "/forgot-password", "/reset-password", "/css/**").permitAll()
                        // Second sign-in step: the password was already checked; the parked
                        // authentication in this session is what the code completes
                        .requestMatchers("/login/otp", "/login/otp/resend").permitAll()
                        // All other requests require authentication
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        // Hands off to the one-time-code step when the user's policy asks for it
                        .successHandler(otpLoginSuccessHandler)
                        .permitAll()
                )
                .requestCache(cache -> cache.requestCache(requestCache))
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
     * Where the pending {@code /oauth2/authorize} request is kept while the user signs in. Shared
     * with the one-time-code step so it resumes the same request after the code is accepted.
     */
    @Bean
    public RequestCache requestCache() {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        cache.setMatchingRequestParameterName(null);
        cache.setRequestMatcher(request -> {
            String uri = request.getRequestURI();
            // Don't cache Chrome DevTools or .well-known requests
            return !uri.startsWith("/.well-known") && !uri.contains("appspecific");
        });
        return cache;
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
