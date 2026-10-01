package com.outreach.platform.auth.service.otp;

import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.auth.service.IdentityEventPublisher;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.core.annotation.Order;

import java.time.Instant;

/** Emails the code (through the outbox to notification-service). The default channel. */
@Named
@Order(0)
public class EmailOtpSender implements OtpSender {

    private final IdentityEventPublisher events;

    @Inject
    public EmailOtpSender(IdentityEventPublisher events) {
        this.events = events;
    }

    @Override
    public String channel() {
        return "EMAIL";
    }

    @Override
    public boolean canReach(UserAccount account) {
        return account.getEmail() != null && !account.getEmail().isBlank();
    }

    @Override
    public String maskedDestination(UserAccount account) {
        String email = account.getEmail();
        int at = email.indexOf('@');
        return at <= 1 ? "•••" + email.substring(Math.max(at, 0)) : email.charAt(0) + "•••" + email.substring(at);
    }

    @Override
    public void send(UserAccount account, OtpPurpose purpose, String code, Instant expiresAt) {
        events.otpIssued(account, channel(), purpose.name(), code, expiresAt);
    }
}
