package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.AuthServiceProperties;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates passwords against the configured policy.
 * Policy mirrors the Keycloak realm: 12 chars, uppercase, lowercase, digit, special char.
 *
 * <p>Only active when {@code idp.provider=spring}. When Keycloak is the active provider,
 * password policy is enforced by the Keycloak realm configuration.
 */
@Named
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class PasswordPolicyValidator {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    private final AuthServiceProperties.SecurityProperties.PasswordPolicy policy;

    @Inject
    public PasswordPolicyValidator(AuthServiceProperties properties) {
        this.policy = properties.security().passwordPolicy();
    }

    /**
     * Validates the given password against the configured policy.
     *
     * @param password the password to validate
     * @return list of violation messages; empty if the password is compliant
     */
    public List<String> validate(String password) {
        List<String> violations = new ArrayList<>();

        if (password == null || password.length() < policy.minLength()) {
            violations.add("Password must be at least %d characters long".formatted(policy.minLength()));
        }

        if (password != null) {
            if (policy.requireUppercase() && !UPPERCASE_PATTERN.matcher(password).find()) {
                violations.add("Password must contain at least one uppercase letter");
            }
            if (policy.requireLowercase() && !LOWERCASE_PATTERN.matcher(password).find()) {
                violations.add("Password must contain at least one lowercase letter");
            }
            if (policy.requireDigit() && !DIGIT_PATTERN.matcher(password).find()) {
                violations.add("Password must contain at least one digit");
            }
            if (policy.requireSpecialChar() && !SPECIAL_CHAR_PATTERN.matcher(password).find()) {
                violations.add("Password must contain at least one special character");
            }
        }

        return violations;
    }

    /**
     * Checks whether the password meets the policy requirements.
     *
     * @param password the password to check
     * @return true if the password is compliant
     */
    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }
}
