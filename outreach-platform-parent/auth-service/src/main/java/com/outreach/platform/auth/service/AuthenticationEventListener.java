package com.outreach.platform.auth.service;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;

/**
 * Listens for Spring Security authentication events to enforce account lockout policy.
 *
 * <p>Only active when {@code idp.provider=spring}. When Keycloak is used,
 * authentication event handling is managed within the Keycloak server.
 */
@Named
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class AuthenticationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationEventListener.class);

    private final AccountLockoutService lockoutService;

    @Inject
    public AuthenticationEventListener(AccountLockoutService lockoutService) {
        this.lockoutService = lockoutService;
    }

    @EventListener
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        // Failure recording is handled directly by LockoutAwareAuthenticationProvider.
        // This listener only logs for observability; the provider increments the counter
        // to avoid double-counting when events are also published.
        String username = event.getAuthentication().getName();
        log.debug("Authentication failed for user: {}", username);
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        lockoutService.resetAttempts(username);
    }
}
