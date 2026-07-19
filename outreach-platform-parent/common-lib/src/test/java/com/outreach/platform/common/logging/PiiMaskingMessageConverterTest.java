package com.outreach.platform.common.logging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PiiMaskingMessageConverter}.
 * Validates that PII patterns are detected and redacted in log messages.
 */
class PiiMaskingMessageConverterTest {

    private static final String REDACTED = "***REDACTED***";

    @ParameterizedTest
    @DisplayName("Should mask email addresses")
    @CsvSource({
            "User email is user@example.com, User email is ***REDACTED***",
            "Contact: john.doe+work@company.co.uk, Contact: ***REDACTED***",
            "Emails: a@b.com and c@d.org, Emails: ***REDACTED*** and ***REDACTED***"
    })
    void shouldMaskEmails(String input, String expected) {
        assertThat(PiiMaskingMessageConverter.maskPii(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Should mask phone numbers")
    @CsvSource({
            "Phone: +1-555-123-4567, Phone: ***REDACTED***",
            "Call (555) 123-4567, Call ***REDACTED***",
            "Dial 555.123.4567 now, Dial ***REDACTED*** now"
    })
    void shouldMaskPhoneNumbers(String input, String expected) {
        assertThat(PiiMaskingMessageConverter.maskPii(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("Should mask employee IDs")
    @CsvSource({
            "Employee EMP-12345 logged in, Employee ***REDACTED*** logged in",
            "Processing EMP12345 record, Processing ***REDACTED*** record",
            "Users: EMP-001 and EMP-99999, Users: ***REDACTED*** and ***REDACTED***"
    })
    void shouldMaskEmployeeIds(String input, String expected) {
        assertThat(PiiMaskingMessageConverter.maskPii(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Should handle null and empty input")
    void shouldHandleNullAndEmpty() {
        assertThat(PiiMaskingMessageConverter.maskPii(null)).isNull();
        assertThat(PiiMaskingMessageConverter.maskPii("")).isEmpty();
    }

    @Test
    @DisplayName("Should not alter messages without PII")
    void shouldNotAlterCleanMessages() {
        String message = "Processing batch of 42 records from service-a";
        assertThat(PiiMaskingMessageConverter.maskPii(message)).isEqualTo(message);
    }

    @Test
    @DisplayName("Should mask multiple PII types in one message")
    void shouldMaskMultiplePiiTypes() {
        String input = "Employee EMP-12345 with email admin@corp.com called +1-555-999-0000";
        String result = PiiMaskingMessageConverter.maskPii(input);

        assertThat(result).doesNotContain("EMP-12345");
        assertThat(result).doesNotContain("admin@corp.com");
        assertThat(result).doesNotContain("555-999-0000");
        assertThat(result).contains(REDACTED);
    }
}
