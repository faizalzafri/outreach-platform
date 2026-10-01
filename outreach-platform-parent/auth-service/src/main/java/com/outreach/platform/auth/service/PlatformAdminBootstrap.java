package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.AuthServiceProperties;
import com.outreach.platform.auth.repo.UserAccountRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first platform admin without any SQL: if no account is a platform admin and
 * {@code PLATFORM_ADMIN_EMAIL} is set, an invited platform admin is created and emailed an
 * activation link. Once one exists (activated or not), startup does nothing.
 */
@Named
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class PlatformAdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrap.class);

    private final UserAccountRepository accounts;
    private final AccountAdminService admin;
    private final AuthServiceProperties.BootstrapProperties bootstrap;

    @Inject
    public PlatformAdminBootstrap(UserAccountRepository accounts, AccountAdminService admin,
                                  AuthServiceProperties properties) {
        this.accounts = accounts;
        this.admin = admin;
        this.bootstrap = properties.bootstrap();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = bootstrap.platformAdminEmail();
        if (email == null || email.isBlank() || accounts.existsByPlatformAdminTrue()) {
            return;
        }
        if (accounts.existsByUsername(bootstrap.platformAdminUsername())) {
            log.warn("Platform admin bootstrap skipped: username '{}' belongs to an existing account",
                    bootstrap.platformAdminUsername());
            return;
        }
        var account = admin.invitePlatformAdmin(bootstrap.platformAdminUsername(), email, "system");
        log.info("Invited the first platform admin: username={}, userId={} (activation link emailed)",
                account.getUsername(), account.getId());
    }
}
