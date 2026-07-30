package com.outreach.platform.notification.service;

import com.outreach.platform.notification.config.NotificationServiceProperties;
import com.outreach.platform.notification.model.DeliveryStatus;
import com.outreach.platform.notification.model.EmailDeliveryDocument;
import com.outreach.platform.notification.repo.EmailDeliveryRepository;
import jakarta.inject.Inject;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Handles asynchronous email dispatch via SMTP with exponential backoff retry. */
@Service
public class EmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchService.class);

    private final JavaMailSender mailSender;
    private final EmailDeliveryRepository deliveryRepository;
    private final NotificationServiceProperties properties;

    @Inject
    public EmailDispatchService(JavaMailSender mailSender,
                                EmailDeliveryRepository deliveryRepository,
                                NotificationServiceProperties properties) {
        this.mailSender = mailSender;
        this.deliveryRepository = deliveryRepository;
        this.properties = properties;
    }

    /** Dispatches an email asynchronously, updating delivery status on success or scheduling retry on failure. */
    @Async("emailTaskExecutor")
    public void dispatchEmail(EmailDeliveryDocument delivery) {
        log.info("Dispatching email to {} for event {}", delivery.getRecipientEmail(), delivery.getEventId());

        delivery.setStatus(DeliveryStatus.QUEUED);
        deliveryRepository.save(delivery);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setTo(delivery.getRecipientEmail());
            helper.setSubject(delivery.getSubject());
            helper.setText(delivery.getBody(), true);

            mailSender.send(mimeMessage);

            delivery.setStatus(DeliveryStatus.SENT);
            delivery.setSentAt(Instant.now());
            delivery.setLastAttemptAt(Instant.now());
            delivery.setAttempts(delivery.getAttempts() + 1);
            delivery.setErrorMessage(null);
            deliveryRepository.save(delivery);

            log.info("Email sent successfully to {} for event {}", delivery.getRecipientEmail(), delivery.getEventId());

        } catch (MessagingException e) {
            handleFailure(delivery, e.getMessage());
        } catch (Exception e) {
            handleFailure(delivery, e.getMessage());
        }
    }

    /** Marks delivery as PERMANENTLY_FAILED or schedules retry with backoff. */
    private void handleFailure(EmailDeliveryDocument delivery, String errorMessage) {
        int newAttempts = delivery.getAttempts() + 1;
        delivery.setAttempts(newAttempts);
        delivery.setLastAttemptAt(Instant.now());
        delivery.setErrorMessage(errorMessage);

        if (newAttempts >= properties.maxRetryAttempts()) {
            delivery.setStatus(DeliveryStatus.PERMANENTLY_FAILED);
            delivery.setNextRetryAt(null);
            log.warn("Email permanently failed after {} attempts to {} for event {}",
                    newAttempts, delivery.getRecipientEmail(), delivery.getEventId());
        } else {
            delivery.setStatus(DeliveryStatus.FAILED);
            Instant nextRetry = calculateNextRetry(newAttempts);
            delivery.setNextRetryAt(nextRetry);
            log.warn("Email dispatch failed (attempt {}/{}) to {} for event {}. Next retry at {}",
                    newAttempts, properties.maxRetryAttempts(),
                    delivery.getRecipientEmail(), delivery.getEventId(), nextRetry);
        }

        deliveryRepository.save(delivery);
    }

    /** Calculates next retry time using the configured backoff schedule. */
    Instant calculateNextRetry(int attemptNumber) {
        List<Integer> backoffMinutes = properties.retryBackoffMinutes();
        int index = Math.min(attemptNumber - 1, backoffMinutes.size() - 1);
        int minutes = backoffMinutes.get(index);
        return Instant.now().plus(minutes, ChronoUnit.MINUTES);
    }
}
