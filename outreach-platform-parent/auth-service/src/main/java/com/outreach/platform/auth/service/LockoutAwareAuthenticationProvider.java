package com.outreach.platform.auth.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Custom authentication provider that checks account lockout status before
 * delegating to the standard DAO authentication provider.
 *
 * <p>Records failed authentication attempts directly to ensure the lockout counter
 * is always incremented, regardless of whether Spring Security's event publishing
 * mechanism is active for this filter chain.
 *
 * <p>Only active when {@code idp.provider=spring}. When Keycloak is used,
 * lockout enforcement is handled by the Keycloak realm configuration.
 *
 * <p>Note: This class is NOT annotated with {@code @Named} to prevent Spring Boot
 * from auto-registering it globally. It is instantiated as a {@code @Bean} in
 * {@link com.outreach.platform.auth.config.SecurityConfig} and explicitly wired
 * into the form-login filter chain only.
 */
public class LockoutAwareAuthenticationProvider extends DaoAuthenticationProvider {

    private final AccountLockoutService lockoutService;

    public LockoutAwareAuthenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            AccountLockoutService lockoutService
    ) {
        super(passwordEncoder);
        setUserDetailsService(userDetailsService);
        this.lockoutService = lockoutService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();

        if (lockoutService.isLocked(username)) {
            throw new LockedException("Account is locked due to too many failed attempts. Try again later.");
        }

        try {
            Authentication result = super.authenticate(authentication);
            // Successful authentication — reset lockout counter
            lockoutService.resetAttempts(username);
            return result;
        } catch (AuthenticationException ex) {
            lockoutService.recordFailedAttempt(username);
            throw ex;
        } catch (IllegalArgumentException ex) {
            // DelegatingPasswordEncoder throws IllegalArgumentException when stored password
            // has no encoding prefix — treat as a failed authentication attempt
            lockoutService.recordFailedAttempt(username);
            throw new BadCredentialsException("Authentication failed", ex);
        }
    }
}
