package com.outreach.platform.auth.model;

/** Actions that can require a one-time passcode, each configurable per tenant. */
public enum OtpPurpose {
    LOGIN,
    PASSWORD_RESET,
    PASSWORD_CHANGE
}
