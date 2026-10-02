package com.outreach.platform.auth.service.otp;

import com.outreach.platform.auth.config.AuthServiceProperties.SecurityProperties.OtpSettings;
import com.outreach.platform.auth.entity.OtpChallenge;
import com.outreach.platform.auth.entity.UserAccount;
import com.outreach.platform.auth.model.OtpPurpose;
import com.outreach.platform.auth.repo.OtpChallengeRepository;
import com.outreach.platform.auth.service.SecurityPolicyService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies one-time passcodes. Whether a purpose needs one, and the code length,
 * lifetime, attempt limit and resend cooldown, come from the account's policy
 * ({@link SecurityPolicyService}); delivery goes through the first {@link OtpSender} that can
 * reach the account.
 *
 * <p>A code is single-use, stored only as a salted SHA-256, compared in constant time, and dies
 * after its attempt limit. Issuing a new code retires the previous open one for that purpose.
 */
@Named
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpChallengeRepository challenges;
    private final SecurityPolicyService policies;
    private final List<OtpSender> senders;

    @Inject
    public OtpService(OtpChallengeRepository challenges, SecurityPolicyService policies, List<OtpSender> senders) {
        this.challenges = challenges;
        this.policies = policies;
        this.senders = senders;
    }

    public boolean isRequired(UserAccount account, OtpPurpose purpose) {
        return policies.otpSettings(account, purpose).enabled();
    }

    public record Issued(String channel, String destination, Instant expiresAt) {
    }

    /**
     * Sends a fresh code, unless the previous one was sent less than the resend cooldown ago.
     *
     * @throws OtpCooldownException when asked again too soon
     * @throws IllegalStateException when no channel can reach the account
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Issued issue(UserAccount account, OtpPurpose purpose) {
        OtpSettings settings = policies.otpSettings(account, purpose);
        Instant now = Instant.now();
        Optional<OtpChallenge> latest = challenges.findFirstByUserIdAndPurposeOrderByCreatedDateDesc(account.getId(), purpose);
        if (latest.isPresent()) {
            Instant allowedAt = latest.get().getCreatedDate().plus(settings.resendCooldown());
            if (allowedAt.isAfter(now)) {
                throw new OtpCooldownException(Duration.between(now, allowedAt).toSeconds() + 1);
            }
        }
        OtpSender sender = senders.stream()
                .filter(s -> s.canReach(account))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No way to send this account a code"));

        retireOpen(account.getId(), purpose, now);

        String code = randomDigits(settings.length());
        OtpChallenge challenge = new OtpChallenge();
        challenge.setUserId(account.getId());
        challenge.setPurpose(purpose);
        challenge.setChannel(sender.channel());
        challenge.setCodeHash("pending");
        challenge.setExpiresAt(now.plus(settings.ttl()));
        challenge.setMaxAttempts(settings.maxAttempts());
        challenges.saveAndFlush(challenge);
        // The generated id salts the hash, so equal codes never share a stored value.
        challenge.setCodeHash(hash(challenge.getId(), code));

        sender.send(account, purpose, code, challenge.getExpiresAt());
        log.info("One-time code issued: purpose={}, channel={}, userId={}", purpose, sender.channel(), account.getId());
        return new Issued(sender.channel(), sender.maskedDestination(account), challenge.getExpiresAt());
    }

    /** Sends a code only if there is no open one already (e.g. when a page is reloaded). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Issued> issueIfNoneOpen(UserAccount account, OtpPurpose purpose) {
        Instant now = Instant.now();
        boolean open = challenges.findFirstByUserIdAndPurposeOrderByCreatedDateDesc(account.getId(), purpose)
                .filter(c -> c.isOpen(now))
                .isPresent();
        return open ? Optional.empty() : Optional.of(issue(account, purpose));
    }

    public enum Outcome { ACCEPTED, WRONG, EXPIRED }

    /**
     * Checks a code against the latest open challenge. A wrong code uses up an attempt; once the
     * attempts run out the challenge is dead and the result is {@link Outcome#EXPIRED}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RuntimeException.class)
    public Verification verify(UserAccount account, OtpPurpose purpose, String code) {
        Instant now = Instant.now();
        Optional<OtpChallenge> latest = challenges.findFirstByUserIdAndPurposeOrderByCreatedDateDesc(account.getId(), purpose)
                .filter(c -> c.isOpen(now));
        if (latest.isEmpty()) {
            return new Verification(Outcome.EXPIRED, 0);
        }
        OtpChallenge challenge = latest.get();
        String candidate = code == null ? "" : code.replaceAll("\\s", "");
        if (MessageDigest.isEqual(hash(challenge.getId(), candidate).getBytes(StandardCharsets.UTF_8),
                challenge.getCodeHash().getBytes(StandardCharsets.UTF_8))) {
            challenge.setConsumedAt(now);
            return new Verification(Outcome.ACCEPTED, 0);
        }
        challenge.setAttempts(challenge.getAttempts() + 1);
        int left = challenge.getMaxAttempts() - challenge.getAttempts();
        log.info("Wrong one-time code: purpose={}, userId={}, attemptsLeft={}", purpose, account.getId(), left);
        return left > 0 ? new Verification(Outcome.WRONG, left) : new Verification(Outcome.EXPIRED, 0);
    }

    public record Verification(Outcome outcome, int attemptsLeft) {
        public boolean accepted() {
            return outcome == Outcome.ACCEPTED;
        }
    }

    private void retireOpen(UUID userId, OtpPurpose purpose, Instant now) {
        challenges.findByUserIdAndPurposeAndConsumedAtIsNull(userId, purpose).forEach(c -> c.setConsumedAt(now));
    }

    private static String randomDigits(int length) {
        StringBuilder digits = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            digits.append(RANDOM.nextInt(10));
        }
        return digits.toString();
    }

    private static String hash(UUID challengeId, String code) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((challengeId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class OtpCooldownException extends RuntimeException {
        private final long retryAfterSeconds;

        public OtpCooldownException(long retryAfterSeconds) {
            super("Please wait " + retryAfterSeconds + " seconds before asking for another code");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
