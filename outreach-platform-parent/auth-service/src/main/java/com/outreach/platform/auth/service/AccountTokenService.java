package com.outreach.platform.auth.service;

import com.outreach.platform.auth.entity.AccountToken;
import com.outreach.platform.auth.model.AccountTokenPurpose;
import com.outreach.platform.auth.repo.AccountTokenRepository;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and redeems single-use links (activation, password reset). The raw token exists only in
 * the emailed link; the database holds its SHA-256, so a database leak cannot be replayed.
 * Issuing a new token for a user and purpose revokes their outstanding ones.
 */
@Named
public class AccountTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountTokenRepository tokens;

    @Inject
    public AccountTokenService(AccountTokenRepository tokens) {
        this.tokens = tokens;
    }

    public record IssuedToken(String rawToken, Instant expiresAt) {
    }

    @Transactional
    public IssuedToken issue(UUID userId, AccountTokenPurpose purpose, Duration ttl) {
        revokeOutstanding(userId, purpose);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AccountToken token = new AccountToken();
        token.setUserId(userId);
        token.setPurpose(purpose);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(ttl));
        tokens.save(token);
        return new IssuedToken(raw, token.getExpiresAt());
    }

    /** The unexpired, unused token for this link, without spending it (to render the form). */
    @Transactional(readOnly = true)
    public Optional<AccountToken> findUsable(String rawToken, AccountTokenPurpose purpose) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return tokens.findByTokenHashAndPurpose(hash(rawToken), purpose)
                .filter(token -> token.isUsable(Instant.now()));
    }

    /** Spends the token; empty if it is unknown, used or expired. */
    @Transactional
    public Optional<AccountToken> consume(String rawToken, AccountTokenPurpose purpose) {
        Optional<AccountToken> token = findUsable(rawToken, purpose);
        token.ifPresent(t -> t.setUsedAt(Instant.now()));
        return token;
    }

    @Transactional
    public void revokeOutstanding(UUID userId, AccountTokenPurpose purpose) {
        Instant now = Instant.now();
        tokens.findByUserIdAndPurposeAndUsedAtIsNull(userId, purpose).forEach(t -> t.setUsedAt(now));
    }

    static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
