package com.outreach.platform.report.service;

import com.outreach.platform.common.messaging.DomainEventMessage;
import com.outreach.platform.common.messaging.RabbitMqConstants;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that the report-service RabbitMQ listener
 * receives ImportJobCompleted messages and publishes a local Spring event.
 * Uses a minimal Spring Boot application context with only RabbitMQ enabled.
 */
@SpringBootTest(
        classes = RabbitMqEventListenerIT.TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Testcontainers
class RabbitMqEventListenerIT {

    @Container
    static RabbitMQContainer rabbitMq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", rabbitMq::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    @Inject
    private RabbitTemplate rabbitTemplate;

    @Inject
    private TestEventCapture testEventCapture;

    @Test
    void shouldReceiveImportJobCompletedAndPublishLocalEvent() throws InterruptedException {
        // Given: an ImportJobCompleted domain event message
        DomainEventMessage message = new DomainEventMessage(
                "test-event-123",
                "ImportJobCompleted",
                Map.of(
                        "jobId", "job-456",
                        "fileName", "volunteers.xlsx",
                        "status", "COMPLETED",
                        "totalRows", 100,
                        "processedRows", 98,
                        "errorCount", 2
                )
        );

        // When: the message is published to the exchange with the correct routing key
        rabbitTemplate.convertAndSend(
                RabbitMqConstants.EXCHANGE_OUTREACH_EVENTS,
                RabbitMqConstants.ROUTING_KEY_IMPORT_JOB_COMPLETED,
                message
        );

        // Then: the listener should receive it and publish a local ImportJobCompletedEvent
        boolean received = testEventCapture.latch.await(10, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(testEventCapture.capturedEvent.get()).isNotNull();
        assertThat(testEventCapture.capturedEvent.get().jobId()).isEqualTo("job-456");
    }

    /**
     * Minimal Spring Boot application for this test — only RabbitMQ.
     * Component scanning is disabled to avoid picking up production beans
     * that require JPA, MongoDB, or Redis dependencies.
     */
    @SpringBootApplication(
            exclude = {
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    MongoAutoConfiguration.class,
                    MongoDataAutoConfiguration.class,
                    RedisAutoConfiguration.class,
                    SecurityAutoConfiguration.class
            }
    )
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @org.springframework.context.annotation.ComponentScan(
            basePackageClasses = RabbitMqEventListenerIT.class,
            useDefaultFilters = false,
            includeFilters = @org.springframework.context.annotation.ComponentScan.Filter(
                    type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
                    classes = RabbitMqEventListenerIT.TestEventCapture.class
            )
    )
    @EnableRabbit
    static class TestApp {

        @Bean
        public MessageConverter jackson2JsonMessageConverter() {
            return new Jackson2JsonMessageConverter();
        }

        @Bean
        public TopicExchange outreachEventsExchange() {
            return new TopicExchange(RabbitMqConstants.EXCHANGE_OUTREACH_EVENTS);
        }

        @Bean
        public Queue reportQueue() {
            return new Queue(RabbitMqConstants.QUEUE_REPORT, true);
        }

        @Bean
        public Binding reportBinding() {
            return BindingBuilder.bind(reportQueue())
                    .to(outreachEventsExchange())
                    .with(RabbitMqConstants.ROUTING_KEY_IMPORT_JOB_COMPLETED);
        }

        @Bean
        public RabbitMqEventListener rabbitMqEventListener(ApplicationEventPublisher publisher) {
            return new RabbitMqEventListener(publisher);
        }
    }

    /**
     * Test helper that captures the local ImportJobCompletedEvent published by the listener.
     */
    @Component
    static class TestEventCapture {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<ImportJobCompletedEvent> capturedEvent = new AtomicReference<>();

        @EventListener
        public void onEvent(ImportJobCompletedEvent event) {
            capturedEvent.set(event);
            latch.countDown();
        }
    }
}
