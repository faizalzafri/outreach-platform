package com.outreach.platform.common.pii;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Deterministic, keyed lookup value for an encrypted PII field (e.g. finding an account by email).
 *
 * <p>AES-GCM ciphertext differs on every write, so it cannot be queried. This HMAC-SHA256 of the
 * trimmed, lower-cased value can: equal inputs give equal outputs, while the PII key keeps it from
 * being brute-forced from a database dump alone.
 */
public final class PiiBlindIndex {

    private static final String ALGORITHM = "HmacSHA256";
    // Domain separation: the HMAC never reuses the key for the same input space as encryption.
    private static final byte[] CONTEXT = "pii-blind-index:".getBytes(StandardCharsets.UTF_8);

    private PiiBlindIndex() {
    }

    public static String of(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(AesEncryptionConverter.getEncryptionKey(), ALGORITHM));
            mac.update(CONTEXT);
            return HexFormat.of().formatHex(
                    mac.doFinal(value.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new PiiEncryptionException("Failed to compute PII blind index", e);
        }
    }
}
