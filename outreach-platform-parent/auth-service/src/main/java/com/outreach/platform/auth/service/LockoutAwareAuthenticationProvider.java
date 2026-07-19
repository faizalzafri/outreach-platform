package com.outreach.platform.auth.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * <p>Only active when {@code idp.provider=spring}. When Keycloak is used,
 * lockout enforcement is handled by the Keycloak realm configuration.
 */
@Named
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class LockoutAwareAuthenticationProvider extends DaoAuthenticationProvider {

    private final AccountLockoutService lockoutService;

    @Inject
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

        return super.authenticate(authentication);
    }
}
