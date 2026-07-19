package com.outreach.platform.common.pii;

/**
 * Classification levels for PII data sensitivity.
 * HIGH: direct identifiers (SSN, full name, email)
 * MEDIUM: indirect identifiers (city, base location, employee ID)
 */
public enum SensitivityLevel {
    HIGH,
    MEDIUM
}
