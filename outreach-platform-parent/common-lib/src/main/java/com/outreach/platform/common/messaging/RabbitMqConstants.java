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

    // ─── DLQ Routing Keys (mirror main keys with .dlq suffix) ───
    public static final String ROUTING_KEY_NOTIFICATION_DLQ = "dlq.notification";
    public static final String ROUTING_KEY_REPORT_DLQ = "dlq.report";

    // ─── Retry Configuration ───
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_INITIAL_INTERVAL_MS = 1000;
    public static final double RETRY_MULTIPLIER = 2.0;
    public static final long RETRY_MAX_INTERVAL_MS = 10000;
}
