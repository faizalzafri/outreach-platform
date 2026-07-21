package com.outreach.platform.notification.model;

/**
 * Lifecycle status of an email delivery attempt.
 */
public enum DeliveryStatus {
    PENDING,
    QUEUED,
    SENT,
    DELIVERED,
    BOUNCED,
    FAILED,
    PERMANENTLY_FAILED
}
