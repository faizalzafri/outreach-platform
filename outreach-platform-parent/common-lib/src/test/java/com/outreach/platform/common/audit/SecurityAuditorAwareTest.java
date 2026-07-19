package com.outreach.platform.common.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditor_authenticatedUser_returnsUsername() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("john.doe", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent().contains("john.doe");
    }

    @Test
    void getCurrentAuditor_noAuthentication_returnsSystem() {
        SecurityContextHolder.clearContext();

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent().contains("system");
    }

    @Test
    void getCurrentAuditor_anonymousUser_returnsSystem() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("anonymousUser", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).isPresent().contains("system");
    }
}
