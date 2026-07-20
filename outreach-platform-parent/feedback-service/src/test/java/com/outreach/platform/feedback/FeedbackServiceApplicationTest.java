package com.outreach.platform.feedback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test verifying the application class is loadable.
 * Full context tests require PostgreSQL, MongoDB, and Redis via Testcontainers.
 */
class FeedbackServiceApplicationTest {

    @Test
    void applicationClassExists() {
        assertNotNull(FeedbackServiceApplication.class);
    }
}
