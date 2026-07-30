package com.outreach.platform.common.health;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.mail.Transport;
import jakarta.mail.Session;

/**
 * Health indicator that verifies SMTP server connectivity by opening and closing a transport session.
 */
@Component
@ConditionalOnProperty("spring.mail.host")
public class SmtpHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(SmtpHealthIndicator.class);

    private final JavaMailSender mailSender;

    @Inject
    public SmtpHealthIndicator(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public Health health() {
        try {
            if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
                Session session = impl.getSession();
                String host = impl.getHost();
                int port = impl.getPort();

                Transport transport = session.getTransport("smtp");
                try {
                    transport.connect(host, port, impl.getUsername(), impl.getPassword());
                } finally {
                    transport.close();
                }

                return Health.up()
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .build();
            }

            // Fallback: if not JavaMailSenderImpl, just report unknown
            return Health.unknown()
                    .withDetail("reason", "Mail sender type not inspectable")
                    .build();
        } catch (Exception ex) {
            log.warn("SMTP health check failed: {}", ex.getMessage());
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
