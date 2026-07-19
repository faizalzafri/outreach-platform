package com.outreach.platform.common.pii;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class AesEncryptionConverterTest {

    // 64-hex-char key (256 bits) for testing
    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final AesEncryptionConverter converter = new AesEncryptionConverter();

    @BeforeEach
    void setUp() {
        System.setProperty("pii.encryption.key", TEST_KEY);
    }

    @AfterEach
    void tearDown() {
        System.setProperty("pii.encryption.key", TEST_KEY);
    }

    @Test
    void encryptDecrypt_roundTrip_returnsOriginal() {
        String original = "john.doe@example.com";
        String encrypted = converter.convertToDatabaseColumn(original);
        String decrypted = converter.convertToEntityAttribute(encrypted);

        assertThat(decrypted).isEqualTo(original);
        assertThat(encrypted).isNotEqualTo(original);
    }

    @Test
    void encrypt_nullInput_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void encrypt_emptyInput_returnsEmpty() {
        assertThat(converter.convertToDatabaseColumn("")).isEmpty();
    }

    @Test
    void decrypt_nullInput_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void decrypt_emptyInput_returnsEmpty() {
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }

    @Test
    void encrypt_sameValueTwice_producesDifferentCiphertext() {
        String original = "sensitive-data";
        String encrypted1 = converter.convertToDatabaseColumn(original);
        String encrypted2 = converter.convertToDatabaseColumn(original);

        // Due to random IV, encrypting the same value should produce different ciphertext
        assertThat(encrypted1).isNotEqualTo(encrypted2);

        // But both should decrypt to the same value
        assertThat(converter.convertToEntityAttribute(encrypted1)).isEqualTo(original);
        assertThat(converter.convertToEntityAttribute(encrypted2)).isEqualTo(original);
    }

    @Test
    void encrypt_withMissingKey_throwsException() {
        // Skip if PII_ENCRYPTION_KEY env var is set (can't clear env vars in test)
        assumeTrue(System.getenv("PII_ENCRYPTION_KEY") == null,
                "Cannot test missing key when PII_ENCRYPTION_KEY env var is set");

        System.clearProperty("pii.encryption.key");

        assertThatThrownBy(() -> converter.convertToDatabaseColumn("test"))
                .isInstanceOf(PiiEncryptionException.class)
                .hasRootCauseInstanceOf(PiiEncryptionException.class)
                .rootCause()
                .hasMessageContaining("not configured");
    }
}
