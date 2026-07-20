package com.outreach.platform.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Basic test verifying the Notification Service application class is correctly configured.
 */
class NotificationServiceApplicationTest {

    @Test
    void applicationClassExists() {
        assertDoesNotThrow(() -> Class.forName(
                "com.outreach.platform.notification.NotificationServiceApplication"));
    }
}

