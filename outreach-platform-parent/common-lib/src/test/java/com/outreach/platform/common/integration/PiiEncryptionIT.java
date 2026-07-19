package com.outreach.platform.common.integration;

import com.outreach.platform.common.pii.AesEncryptionConverter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for PII encryption at rest.
 * Verifies AES-256-GCM encryption round-trip: plaintext → encrypt → decrypt → original.
 * Ensures encrypted form is never the same as plaintext.
 */
class PiiEncryptionIT {

    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final AesEncryptionConverter converter = new AesEncryptionConverter();

    @BeforeAll
    static void setupEncryptionKey() {
        System.setProperty("pii.encryption.key", TEST_KEY);
    }

    @AfterAll
    static void cleanupEncryptionKey() {
        System.clearProperty("pii.encryption.key");
    }

    @Test
    @DisplayName("Encrypt-decrypt round-trip preserves email PII")
    void encryptDecrypt_email_roundTrip() {
        String email = "jane.doe@example.com";

        String encrypted = converter.convertToDatabaseColumn(email);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(email);
        assertThat(encrypted).isNotBlank();
        assertThat(decrypted).isEqualTo(email);
    }

    @Test
    @DisplayName("Encrypt-decrypt round-trip preserves phone number PII")
    void encryptDecrypt_phoneNumber_roundTrip() {
        String phone = "+1-555-867-5309";

        String encrypted = converter.convertToDatabaseColumn(phone);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(phone);
        assertThat(decrypted).isEqualTo(phone);
    }

    @Test
    @DisplayName("Encrypt-decrypt round-trip preserves multi-line PII data")
    void encryptDecrypt_multiLineData_roundTrip() {
        String personalData = "Name: John Smith\nEmail: john@company.com\nPhone: 555-123-4567";

        String encrypted = converter.convertToDatabaseColumn(personalData);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(personalData);
        assertThat(encrypted).doesNotContain("John Smith");
        assertThat(decrypted).isEqualTo(personalData);
    }

    @Test
    @DisplayName("Encrypted form does not contain plaintext value")
    void encrypted_doesNotContainPlaintext() {
        String sensitiveValue = "SocialSecurityNumber123";

        String encrypted = converter.convertToDatabaseColumn(sensitiveValue);

        // Encrypted value is Base64 encoded; it should never contain the original value
        assertThat(encrypted).doesNotContain(sensitiveValue);
    }

    @Test
    @DisplayName("Same plaintext encrypted twice produces different ciphertext (random IV)")
    void encrypt_sameValue_differentCiphertext() {
        String pii = "employee-id-12345";

        String encrypted1 = converter.convertToDatabaseColumn(pii);
        String encrypted2 = converter.convertToDatabaseColumn(pii);

        // Random IV ensures different ciphertext each time
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // Both decrypt to original
        assertThat(converter.convertToEntityAttribute(encrypted1)).isEqualTo(pii);
        assertThat(converter.convertToEntityAttribute(encrypted2)).isEqualTo(pii);
    }

    @Test
    @DisplayName("Encryption with special characters round-trips correctly")
    void encryptDecrypt_specialCharacters_roundTrip() {
        String special = "Ñoño García — user+tag@例え.jp «quoted»";

        String encrypted = converter.convertToDatabaseColumn(special);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(encrypted).isNotEqualTo(special);
        assertThat(decrypted).isEqualTo(special);
    }
}
