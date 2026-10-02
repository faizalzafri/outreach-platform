package com.outreach.platform.auth;

import com.outreach.platform.auth.service.PasswordPolicyValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.inject.Inject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for password policy enforcement.
 * Verifies: 12+ chars, uppercase, lowercase, digit, and special character requirements.
 */
@DisplayName("Password Policy Enforcement")
class PasswordPolicyIntegrationTest extends BaseAuthIntegrationTest {

    @Inject
    private PasswordPolicyValidator passwordPolicyValidator;

    @Test
    @DisplayName("Should reject password shorter than 12 characters")
    void shouldRejectShortPassword() {
        List<String> violations = passwordPolicyValidator.validate("Abc@1234567");
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.contains("at least 12 characters"));
    }

    @Test
    @DisplayName("Should reject password missing uppercase letter")
    void shouldRejectPasswordWithoutUppercase() {
        List<String> violations = passwordPolicyValidator.validate("abcdefgh@123");
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.contains("uppercase"));
    }

    @Test
    @DisplayName("Should reject password missing lowercase letter")
    void shouldRejectPasswordWithoutLowercase() {
        List<String> violations = passwordPolicyValidator.validate("ABCDEFGH@123");
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.contains("lowercase"));
    }

    @Test
    @DisplayName("Should reject password missing digit")
    void shouldRejectPasswordWithoutDigit() {
        List<String> violations = passwordPolicyValidator.validate("Abcdefgh@abc");
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.contains("digit"));
    }

    @Test
    @DisplayName("Should reject password missing special character")
    void shouldRejectPasswordWithoutSpecialChar() {
        List<String> violations = passwordPolicyValidator.validate("Abcdefgh1234");
        assertThat(violations)
                .isNotEmpty()
                .anyMatch(v -> v.contains("special character"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short",           // too short
            "nouppercase@1",   // missing uppercase (also too short)
            "NOLOWERCASE@1",   // missing lowercase (also too short)
            "NoDigit@abcde",   // missing digit
            "NoSpecial1abcd"   // missing special char
    })
    @DisplayName("Should reject various weak passwords")
    void shouldRejectWeakPasswords(String weakPassword) {
        List<String> violations = passwordPolicyValidator.validate(weakPassword);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should accept password meeting all requirements (12+ chars, upper, lower, digit, special)")
    void shouldAcceptStrongPassword() {
        List<String> violations = passwordPolicyValidator.validate("StrongP@ss123");
        assertThat(violations).isEmpty();
        assertThat(passwordPolicyValidator.isValid("StrongP@ss123")).isTrue();
    }

    @Test
    @DisplayName("Should reject a common word dressed up with digits and symbols")
    void shouldRejectGuessablePasswords() {
        assertThat(passwordPolicyValidator.validate("Admin@12345!"))
                .containsExactly("Password is too easy to guess; avoid common words with numbers or symbols added");
        assertThat(passwordPolicyValidator.isValid("Password@2026!")).isFalse();
        assertThat(passwordPolicyValidator.isValid("Lotus-Garden-47")).isTrue();
    }

    @Test
    @DisplayName("Should reject a password containing the username or email name")
    void shouldRejectPasswordContainingIdentity() {
        assertThat(passwordPolicyValidator.validate("Priya_Sharma-9x", "priya_sharma", null, 12))
                .contains("Password must not contain your username or email address");
        assertThat(passwordPolicyValidator.validate("Kites!meera.iyer7", "mi", "meera.iyer@example.com", 12))
                .contains("Password must not contain your username or email address");
    }

    @Test
    @DisplayName("A tenant can require a longer minimum than the platform, never a shorter one")
    void tenantMinimumOnlyTightens() {
        assertThat(passwordPolicyValidator.validate("Lotus-Garden-47", null, null, 16))
                .containsExactly("Password must be at least 16 characters long");
        assertThat(passwordPolicyValidator.validate("Lotus-Grd-47", null, null, 8)).isEmpty();
        assertThat(passwordPolicyValidator.validate("Lotus-Gd-47", null, null, 8))
                .containsExactly("Password must be at least 12 characters long");
    }

    @Test
    @DisplayName("Should reject null password")
    void shouldRejectNullPassword() {
        List<String> violations = passwordPolicyValidator.validate(null);
        assertThat(violations).isNotEmpty();
    }

    @Test
    @DisplayName("Should reject empty password")
    void shouldRejectEmptyPassword() {
        List<String> violations = passwordPolicyValidator.validate("");
        assertThat(violations).isNotEmpty();
    }
}
