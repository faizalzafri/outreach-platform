package com.outreach.platform.common.security;

import java.util.regex.Pattern;

/**
 * Utility class for XSS (Cross-Site Scripting) input sanitization.
 * Strips HTML tags, decodes entities, and removes potentially dangerous content
 * before persistence or rendering.
 */
public final class XssSanitizer {

    private XssSanitizer() {
        // Utility class
    }

    // Pattern to match HTML/XML tags
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]*>");

    // Pattern to match HTML entities (named and numeric)
    private static final Pattern HTML_ENTITY_PATTERN = Pattern.compile("&[#a-zA-Z0-9]+;");

    // Pattern to match javascript: protocol in any case
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN =
            Pattern.compile("(?i)javascript\\s*:", Pattern.CASE_INSENSITIVE);

    // Pattern to match on-event handlers (onclick, onload, onerror, etc.)
    private static final Pattern EVENT_HANDLER_PATTERN =
            Pattern.compile("(?i)on\\w+\\s*=", Pattern.CASE_INSENSITIVE);

    // Pattern to match data: URIs that could contain executable content
    private static final Pattern DATA_URI_PATTERN =
            Pattern.compile("(?i)data\\s*:[^,]*;base64", Pattern.CASE_INSENSITIVE);

    /**
     * Sanitizes the input string by removing all HTML tags, event handlers,
     * JavaScript protocols, and decoding HTML entities.
     *
     * @param input the potentially unsafe user input
     * @return sanitized string safe for storage and display, or null if input is null
     */
    public static String sanitize(String input) {
        if (input == null) {
            return null;
        }
        if (input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");

        // Remove javascript: protocol references
        sanitized = JAVASCRIPT_PROTOCOL_PATTERN.matcher(sanitized).replaceAll("");

        // Remove event handlers
        sanitized = EVENT_HANDLER_PATTERN.matcher(sanitized).replaceAll("");

        // Remove data: URIs with base64 content
        sanitized = DATA_URI_PATTERN.matcher(sanitized).replaceAll("");

        // Decode common HTML entities
        sanitized = decodeHtmlEntities(sanitized);

        // Remove any remaining HTML entities
        sanitized = HTML_ENTITY_PATTERN.matcher(sanitized).replaceAll("");

        return sanitized.trim();
    }

    /**
     * Checks if the input contains potentially dangerous XSS content.
     *
     * @param input the string to check
     * @return true if the input contains potentially dangerous content
     */
    public static boolean containsXss(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return HTML_TAG_PATTERN.matcher(input).find()
                || JAVASCRIPT_PROTOCOL_PATTERN.matcher(input).find()
                || EVENT_HANDLER_PATTERN.matcher(input).find()
                || DATA_URI_PATTERN.matcher(input).find();
    }

    private static String decodeHtmlEntities(String input) {
        String decoded = input;
        decoded = decoded.replace("&lt;", "<");
        decoded = decoded.replace("&gt;", ">");
        decoded = decoded.replace("&amp;", "&");
        decoded = decoded.replace("&quot;", "\"");
        decoded = decoded.replace("&#x27;", "'");
        decoded = decoded.replace("&#39;", "'");
        decoded = decoded.replace("&apos;", "'");
        decoded = decoded.replace("&#x2F;", "/");
        decoded = decoded.replace("&#47;", "/");
        return decoded;
    }
}
