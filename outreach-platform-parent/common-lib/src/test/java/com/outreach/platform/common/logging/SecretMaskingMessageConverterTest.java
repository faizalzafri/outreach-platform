package com.outreach.platform.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SecretMaskingMessageConverter}.
 * Validates that secret values are never exposed in log output.
 */
class SecretMaskingMessageConverterTest {

    private static final String REDACTED = "***REDACTED***";

    @ParameterizedTest
    @DisplayName("Should mask secret key=value patterns")
    @CsvSource({
            "Config: password=s3cr3t123, Config: password=***REDACTED***",
            "Using token=abc123xyz end, Using token=***REDACTED*** end",
            "Set apiKey=my-api-key-here!, Set apiKey=***REDACTED***"
    })
    void shouldMaskSecrets(String input, String expected) {
        assertThat(SecretMaskingMessageConverter.maskSecrets(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should mask case-insensitively")
    void shouldMaskCaseInsensitive() {
        String input = "PASSWORD=MyP@ss and Secret=hidden";
        String result = SecretMaskingMessageConverter.maskSecrets(input);

        assertThat(result).doesNotContain("MyP@ss");
        assertThat(result).doesNotContain("hidden");
        assertThat(result).contains(REDACTED);
    }

    @Test
    @DisplayName("Should mask quoted secret values")
    void shouldMaskQuotedValues() {
        String input = "password=\"super-secret\" and token='my-token'";
        String result = SecretMaskingMessageConverter.maskSecrets(input);

        assertThat(result).doesNotContain("super-secret");
        assertThat(result).doesNotContain("my-token");
    }

    @Test
    @DisplayName("Should mask client_secret and api_key patterns")
    void shouldMaskUnderscoreVariants() {
        String input = "client_secret=abc123 api_key=xyz789";
        String result = SecretMaskingMessageConverter.maskSecrets(input);

        assertThat(result).doesNotContain("abc123");
        assertThat(result).doesNotContain("xyz789");
    }

    @Test
    @DisplayName("Should handle null and empty input")
    void shouldHandleNullAndEmpty() {
        assertThat(SecretMaskingMessageConverter.maskSecrets(null)).isNull();
        assertThat(SecretMaskingMessageConverter.maskSecrets("")).isEmpty();
    }

    @Test
    @DisplayName("Should not alter messages without secrets")
    void shouldNotAlterCleanMessages() {
        String message = "User authenticated successfully with role ADMIN";
        assertThat(SecretMaskingMessageConverter.maskSecrets(message)).isEqualTo(message);
    }
}
