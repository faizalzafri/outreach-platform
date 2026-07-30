package com.outreach.platform.common.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Shared RabbitMQ infrastructure: declares exchanges, queues with DLQ, bindings, retry policy, and JSON message converter.
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.amqp.rabbit.core.RabbitTemplate")
@ConditionalOnProperty(name = "spring.rabbitmq.host")
public class RabbitMqConfig {

    // ═══════════════════════════════════════════════════════════════
    // Exchanges
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public TopicExchange outreachEventsExchange() {
        return new TopicExchange(RabbitMqConstants.EXCHANGE_OUTREACH_EVENTS);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(RabbitMqConstants.EXCHANGE_DEAD_LETTER);
    }

    // ═══════════════════════════════════════════════════════════════
    // Main Queues (with DLX arguments)
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_NOTIFICATION)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.ROUTING_KEY_NOTIFICATION_DLQ)
                .build();
    }

    @Bean
    public Queue reportQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_REPORT)
                .withArgument("x-dead-letter-exchange", RabbitMqConstants.EXCHANGE_DEAD_LETTER)
                .withArgument("x-dead-letter-routing-key", RabbitMqConstants.ROUTING_KEY_REPORT_DLQ)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // Dead Letter Queues
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public Queue notificationDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_NOTIFICATION_DLQ).build();
    }

    @Bean
    public Queue reportDeadLetterQueue() {
        return QueueBuilder.durable(RabbitMqConstants.QUEUE_REPORT_DLQ).build();
    }

    // ═══════════════════════════════════════════════════════════════
    // Bindings: Main queues → Topic exchange
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public Binding notificationBindingStatusChanged() {
        return BindingBuilder.bind(notificationQueue())
                .to(outreachEventsExchange())
                .with(RabbitMqConstants.ROUTING_KEY_EVENT_STATUS_CHANGED);
    }

    @Bean
    public Binding notificationBindingVolunteersImported() {
        return BindingBuilder.bind(notificationQueue())
                .to(outreachEventsExchange())
                .with(RabbitMqConstants.ROUTING_KEY_VOLUNTEERS_IMPORTED);
    }

    @Bean
    public Binding notificationBindingSendFeedbackEmails() {
        return BindingBuilder.bind(notificationQueue())
                .to(outreachEventsExchange())
                .with(RabbitMqConstants.ROUTING_KEY_SEND_FEEDBACK_EMAILS);
    }

    @Bean
    public Binding reportBindingImportJobCompleted() {
        return BindingBuilder.bind(reportQueue())
                .to(outreachEventsExchange())
                .with(RabbitMqConstants.ROUTING_KEY_IMPORT_JOB_COMPLETED);
    }

    // ═══════════════════════════════════════════════════════════════
    // Bindings: Dead letter queues → DLX
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public Binding notificationDlqBinding() {
        return BindingBuilder.bind(notificationDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMqConstants.ROUTING_KEY_NOTIFICATION_DLQ);
    }

    @Bean
    public Binding reportDlqBinding() {
        return BindingBuilder.bind(reportDeadLetterQueue())
                .to(deadLetterExchange())
                .with(RabbitMqConstants.ROUTING_KEY_REPORT_DLQ);
    }

    // ═══════════════════════════════════════════════════════════════
    // Message Converter (JSON via Jackson)
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ═══════════════════════════════════════════════════════════════
    // RabbitTemplate with retry
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                        MessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter);
        template.setRetryTemplate(retryTemplate());
        return template;
    }

    // ═══════════════════════════════════════════════════════════════
    // Listener container factory with retry (consumer-side)
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jackson2JsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter);
        factory.setDefaultRequeueRejected(false); // rejected messages go to DLQ, not requeued
        factory.setPrefetchCount(10);
        return factory;
    }

    // ═══════════════════════════════════════════════════════════════
    // Retry Template (exponential backoff)
    // ═══════════════════════════════════════════════════════════════

    private RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(RabbitMqConstants.RETRY_INITIAL_INTERVAL_MS);
        backOff.setMultiplier(RabbitMqConstants.RETRY_MULTIPLIER);
        backOff.setMaxInterval(RabbitMqConstants.RETRY_MAX_INTERVAL_MS);
        retryTemplate.setBackOffPolicy(backOff);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(RabbitMqConstants.MAX_RETRY_ATTEMPTS);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
