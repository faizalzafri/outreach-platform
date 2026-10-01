package com.outreach.platform.auth.service;

import com.outreach.platform.auth.config.AuthServiceProperties;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Checks a candidate password against the password policy: length, character classes, and
 * guessability (common words, the user's own username or email). Reuse of previous passwords is
 * checked by {@link PasswordService}, which has the stored hashes.
 */
@Named
@ConditionalOnProperty(name = "idp.provider", havingValue = "spring")
public class PasswordPolicyValidator {

    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");
    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z]");

    private final AuthServiceProperties.SecurityProperties.PasswordPolicy policy;
    private final Set<String> commonWords;

    @Inject
    public PasswordPolicyValidator(AuthServiceProperties properties) {
        this.policy = properties.security().passwordPolicy();
        this.commonWords = loadCommonWords();
    }

    public List<String> validate(String password) {
        return validate(password, null, null, policy.minLength());
    }

    /**
     * @param minLength the effective minimum (a tenant may require more than the platform default)
     */
    public List<String> validate(String password, String username, String email, int minLength) {
        List<String> violations = new ArrayList<>();
        int requiredLength = Math.max(minLength, policy.minLength());

        if (password == null || password.length() < requiredLength) {
            violations.add("Password must be at least %d characters long".formatted(requiredLength));
        }
        if (password == null) {
            return violations;
        }
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

        String lower = password.toLowerCase(Locale.ROOT);
        if (policy.rejectCommonPasswords() && commonWords.contains(NON_LETTERS.matcher(lower).replaceAll(""))) {
            violations.add("Password is too easy to guess; avoid common words with numbers or symbols added");
        }
        if (containsIdentity(lower, username) || containsIdentity(lower, localPart(email))) {
            violations.add("Password must not contain your username or email address");
        }
        return violations;
    }

    public boolean isValid(String password) {
        return validate(password).isEmpty();
    }

    private static boolean containsIdentity(String lowerPassword, String identity) {
        return identity != null && identity.length() >= 3 && lowerPassword.contains(identity.toLowerCase(Locale.ROOT));
    }

    private static String localPart(String email) {
        return email == null ? null : email.substring(0, Math.max(email.indexOf('@'), 0));
    }

    private static Set<String> loadCommonWords() {
        try {
            String text = new ClassPathResource("security/common-password-words.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
            return text.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("Password word list missing", e);
        }
    }
}
