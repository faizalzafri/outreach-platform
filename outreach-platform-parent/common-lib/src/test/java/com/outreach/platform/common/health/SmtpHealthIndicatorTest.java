package com.outreach.platform.common.health;

import jakarta.mail.Session;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmtpHealthIndicator")
class SmtpHealthIndicatorTest {

    private SmtpHealthIndicator indicator;

    @Mock
    private JavaMailSenderImpl mailSender;

    @BeforeEach
    void setUp() {
        indicator = new SmtpHealthIndicator(mailSender);
    }

    @Test
    @DisplayName("returns DOWN when getSession throws exception")
    void returnsDownWhenSessionThrows() {
        when(mailSender.getSession()).thenThrow(new RuntimeException("Connection refused"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
        assertThat(health.getDetails().get("error")).asString().contains("Connection refused");
    }

    @Test
    @DisplayName("returns DOWN when transport connect fails (no real SMTP server)")
    void returnsDownWhenTransportConnectFails() {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        Session session = Session.getInstance(props);

        when(mailSender.getSession()).thenReturn(session);
        when(mailSender.getHost()).thenReturn("smtp.example.com");
        when(mailSender.getPort()).thenReturn(587);
        when(mailSender.getUsername()).thenReturn("user");
        when(mailSender.getPassword()).thenReturn("pass");

        Health health = indicator.health();

        // Without a real SMTP server, the transport.connect() will fail
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    @DisplayName("error detail includes meaningful message on failure")
    void errorDetailIncludesMeaningfulMessage() {
        when(mailSender.getSession()).thenThrow(new RuntimeException("SMTP server timeout"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error")).asString().contains("SMTP server timeout");
    }
}
