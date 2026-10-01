package com.outreach.platform.auth.service.otp;

import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.OtpPurpose;

import java.time.Instant;

/**
 * Delivers a one-time code over one channel. {@link OtpService} issues and verifies codes and
 * knows nothing about delivery, so adding SMS or push means adding an implementation (and, in
 * notification-service, a matching channel sender); issuing and verification stay as they are.
 */
public interface OtpSender {

    /** Channel name carried on the event, e.g. {@code EMAIL}, {@code SMS}, {@code PUSH}. */
    String channel();

    /** Whether this account has what the channel needs (an email address, a phone number, a device). */
    boolean canReach(UserAccount account);

    /** A short description of where the code went, safe to show on screen (e.g. {@code p•••@example.com}). */
    String maskedDestination(UserAccount account);

    void send(UserAccount account, OtpPurpose purpose, String code, Instant expiresAt);
}
