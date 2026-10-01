package com.outreach.platform.common.messaging;

/**
 * Shared constants for RabbitMQ messaging across all services.
 * Defines exchange names, queue names, routing keys, and dead letter configuration.
 */
public final class RabbitMqConstants {

    private RabbitMqConstants() {
        // utility class
    }

    // ─── Main Exchange ───
    public static final String EXCHANGE_OUTREACH_EVENTS = "outreach.events";

    // ─── Dead Letter Exchange ───
    public static final String EXCHANGE_DEAD_LETTER = "outreach.events.dlx";

    // ─── Main Queues ───
    public static final String QUEUE_NOTIFICATION = "outreach.notification.queue";
    public static final String QUEUE_REPORT = "outreach.report.queue";

    // ─── Dead Letter Queues ───
    public static final String QUEUE_NOTIFICATION_DLQ = "outreach.notification.dlq";
    public static final String QUEUE_REPORT_DLQ = "outreach.report.dlq";

    // ─── Routing Keys ───
    public static final String ROUTING_KEY_EVENT_STATUS_CHANGED = "event.status-changed";
    public static final String ROUTING_KEY_VOLUNTEERS_IMPORTED = "event.volunteers-imported";
    public static final String ROUTING_KEY_SEND_FEEDBACK_EMAILS = "event.send-feedback-emails";
    public static final String ROUTING_KEY_IMPORT_JOB_COMPLETED = "event.import-job-completed";

    // ─── Identity (published by auth-service) ───
    public static final String ROUTING_KEY_IDENTITY_USER_INVITED = "identity.user-invited";
    public static final String ROUTING_KEY_IDENTITY_PASSWORD_RESET_REQUESTED = "identity.password-reset-requested";
    public static final String ROUTING_KEY_IDENTITY_PASSWORD_CHANGED = "identity.password-changed";
    public static final String ROUTING_KEY_IDENTITY_OTP_ISSUED = "identity.otp-issued";
    public static final String ROUTING_KEY_IDENTITY_USER_CHANGED = "identity.user-changed";

    /** Identity events consumed by event-service to keep its user directory in sync. */
    public static final String QUEUE_EVENT_IDENTITY = "outreach.event.identity.queue";
    public static final String QUEUE_EVENT_IDENTITY_DLQ = "outreach.event.identity.dlq";
    public static final String ROUTING_KEY_EVENT_IDENTITY_DLQ = "dlq.event.identity";

    /**
     * {@code x-tenant-id} value for messages that belong to no tenant (e.g. inviting the first
     * platform admin). Consumers require the header on every message.
     */
    public static final java.util.UUID PLATFORM_TENANT_ID = new java.util.UUID(0L, 0L);

    // ─── DLQ Routing Keys (mirror main keys with .dlq suffix) ───
    public static final String ROUTING_KEY_NOTIFICATION_DLQ = "dlq.notification";
    public static final String ROUTING_KEY_REPORT_DLQ = "dlq.report";

    // ─── Retry Configuration ───
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_INITIAL_INTERVAL_MS = 1000;
    public static final double RETRY_MULTIPLIER = 2.0;
    public static final long RETRY_MAX_INTERVAL_MS = 10000;
}
