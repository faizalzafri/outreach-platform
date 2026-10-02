package com.outreach.platform.feedback.model;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The one list of feedback categories. The submission form, the list filters and validation all
 * use it (served at {@code GET /feedback/categories}); stored values are the labels.
 */
public enum FeedbackCategory {
    COMMUNICATION("Communication"),
    ORGANIZATION("Organization"),
    CONTENT("Content"),
    LOGISTICS("Logistics"),
    TEAMWORK("Teamwork"),
    LEADERSHIP("Leadership"),
    IMPACT("Impact"),
    SAFETY("Safety"),
    OVERALL("Overall");

    private final String label;

    FeedbackCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(FeedbackCategory::label).toList();
    }

    /**
     * The stored label for a submitted category, matched case-insensitively. Blank means none;
     * the older "General" means {@link #OVERALL}.
     *
     * @throws IllegalArgumentException for anything that is not a category
     */
    public static String normalize(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String value = category.trim();
        if ("general".equalsIgnoreCase(value)) {
            return OVERALL.label;
        }
        return Arrays.stream(values())
                .filter(c -> c.label.equalsIgnoreCase(value) || c.name().equals(value.toUpperCase(Locale.ROOT)))
                .findFirst()
                .map(FeedbackCategory::label)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown feedback category '" + value + "'; use one of " + labels()));
    }
}
