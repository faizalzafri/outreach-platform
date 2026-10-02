package com.outreach.platform.auth;

import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the application context starts successfully
 * with all beans wired correctly (auth server, security, datasource).
 */
class AuthServiceApplicationTest extends BaseAuthIntegrationTest {

    @Inject
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        // Verifies the application context starts successfully
        // with all beans wired correctly (auth server, security, datasource)
    }

    @Test
    void outboxPublishesThroughTheSharedJsonTemplate() {
        // auth-service doesn't component-scan common-lib; without the auto-configuration it got
        // Boot's plain template, which cannot serialize identity events.
        assertThat(rabbitTemplate.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
    }
}
