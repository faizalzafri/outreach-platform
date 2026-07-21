package com.outreach.platform.common.messaging;

/**
 * Shared constants for RabbitMQ messaging across all services.
 * Defines exchange names, queue names, and routing keys.
 */
public final class RabbitMqConstants {

    private RabbitMqConstants() {
        // utility class
    }

    // Exchange
    public static final String EXCHANGE_OUTREACH_EVENTS = "outreach.events";

    // Queues
    public static final String QUEUE_NOTIFICATION = "outreach.notification.queue";
    public static final String QUEUE_REPORT = "outreach.report.queue";

    // Routing Keys
    public static final String ROUTING_KEY_EVENT_STATUS_CHANGED = "event.status-changed";
    public static final String ROUTING_KEY_VOLUNTEERS_IMPORTED = "event.volunteers-imported";
    public static final String ROUTING_KEY_SEND_FEEDBACK_EMAILS = "event.send-feedback-emails";
    public static final String ROUTING_KEY_IMPORT_JOB_COMPLETED = "event.import-job-completed";
}
