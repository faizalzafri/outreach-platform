package com.outreach.platform.common.pii;

/**
 * Runtime exception thrown when PII encryption or decryption operations fail.
 */
public class PiiEncryptionException extends RuntimeException {

    public PiiEncryptionException(String message) {
        super(message);
    }

    public PiiEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
