package com.outreach.platform.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared RabbitMQ infrastructure configuration.
 * Declares the topic exchange, queues, and bindings used across services.
 * Only activates when spring-boot-starter-amqp is on the classpath.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMqConfig {

    // --- Exchange ---

    @Bean
    public TopicExchange outreachEventsExchange() {
        return new TopicExchange(RabbitMqConstants.EXCHANGE_OUTREACH_EVENTS);
    }

    // --- Queues ---

    @Bean
    public Queue notificationQueue() {
        return new Queue(RabbitMqConstants.QUEUE_NOTIFICATION, true);
    }

    @Bean
    public Queue reportQueue() {
        return new Queue(RabbitMqConstants.QUEUE_REPORT, true);
    }

    // --- Bindings: Notification Queue ---

    @Bean
    public Binding notificationBindingStatusChanged(Queue notificationQueue, TopicExchange outreachEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(outreachEventsExchange)
                .with(RabbitMqConstants.ROUTING_KEY_EVENT_STATUS_CHANGED);
    }

    @Bean
    public Binding notificationBindingVolunteersImported(Queue notificationQueue, TopicExchange outreachEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(outreachEventsExchange)
                .with(RabbitMqConstants.ROUTING_KEY_VOLUNTEERS_IMPORTED);
    }

    @Bean
    public Binding notificationBindingSendFeedbackEmails(Queue notificationQueue, TopicExchange outreachEventsExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(outreachEventsExchange)
                .with(RabbitMqConstants.ROUTING_KEY_SEND_FEEDBACK_EMAILS);
    }

    // --- Bindings: Report Queue ---

    @Bean
    public Binding reportBindingImportJobCompleted(Queue reportQueue, TopicExchange outreachEventsExchange) {
        return BindingBuilder.bind(reportQueue)
                .to(outreachEventsExchange)
                .with(RabbitMqConstants.ROUTING_KEY_IMPORT_JOB_COMPLETED);
    }

    // --- Message Converter (JSON via Jackson) ---

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
