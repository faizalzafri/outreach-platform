package com.outreach.platform.auth.model;

/**
 * Lifecycle of a sign-in account. {@code INVITED} accounts have no password until the user
 * activates them from the emailed link; only {@code ACTIVE} accounts can sign in.
 */
public enum AccountStatus {
    INVITED,
    ACTIVE,
    DISABLED
}
