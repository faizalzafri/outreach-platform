package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.repo.TenantMembershipRepository;
import com.outreach.platform.auth.repo.UserAccountRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Loads sign-in accounts from {@link UserAccount}. Authorities are {@code ROLE_PLATFORM_ADMIN} for
 * platform admins, otherwise {@code ROLE_<role>} for every tenant membership; the token customizer
 * narrows them to the active tenant.
 */
@Named
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class AccountUserDetailsService implements UserDetailsService {

    public static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_PLATFORM_ADMIN";

    private final UserAccountRepository accounts;
    private final TenantMembershipRepository memberships;

    @Inject
    public AccountUserDetailsService(UserAccountRepository accounts, TenantMembershipRepository memberships) {
        this.accounts = accounts;
        this.memberships = memberships;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserAccount account = accounts.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        List<SimpleGrantedAuthority> authorities = account.isPlatformAdmin()
                ? List.of(new SimpleGrantedAuthority(PLATFORM_ADMIN_AUTHORITY))
                : memberships.findByUserId(account.getId()).stream()
                        .map(m -> "ROLE_" + m.getRole().name())
                        .distinct()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        return User.withUsername(account.getUsername())
                // An invited account has no password yet; it is also disabled, so it never gets this far.
                .password(account.getPassword() != null ? account.getPassword() : "")
                .disabled(!account.isEnabled())
                .accountLocked(account.isLocked(Instant.now()))
                .authorities(authorities)
                .build();
    }
}
