package com.outreach.platform.common.integration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for log masking: verifies that PII and secrets are never present
 * in actual Logback output when using the MaskingCompositeConverter.
 *
 * Uses a PatternLayout with the %maskedMsg conversion word (same as production config)
 * to capture real log output and verify masking occurs end-to-end through the Logback pipeline.
 */
class LogMaskingIT {

    private static final String REDACTED = "***REDACTED***";

    private Logger logger;
    private ByteArrayOutputStream outputStream;
    private OutputStreamAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Create a PatternLayout with maskedMsg converter registered
        PatternLayout layout = new PatternLayout();
        layout.setContext(loggerContext);

        // Register the conversion rule the same way logback-spring.xml does
        Map<String, String> converterMap = new HashMap<>();
        converterMap.put("maskedMsg", "com.outreach.platform.common.logging.MaskingCompositeConverter");
        layout.setPattern("%maskedMsg%n");
        layout.getInstanceConverterMap().putAll(converterMap);
        layout.start();

        // Wrap the layout in an encoder
        LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
        encoder.setContext(loggerContext);
        encoder.setLayout(layout);
        encoder.start();

        outputStream = new ByteArrayOutputStream();

        appender = new OutputStreamAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.setOutputStream(outputStream);
        appender.start();

        logger = loggerContext.getLogger("com.outreach.platform.common.integration.LogMaskingIT");
        logger.addAppender(appender);
        logger.setAdditive(false);
    }

    @AfterEach
    void tearDown() {
        if (appender != null) {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private String getCapturedOutput() {
        return outputStream.toString(StandardCharsets.UTF_8);
    }

    // ===== Secret Masking Tests =====

    @Test
    @DisplayName("Secret password=value is redacted in log output")
    void secretPassword_isRedacted() {
        logger.info("Config loaded: password=mysecret123 for service");

        String output = getCapturedOutput();
        assertThat(output).doesNotContain("mysecret123");
        assertThat(output).contains(REDACTED);
    }

    @Test
    @DisplayName("Secret token=value is redacted in log output")
    void secretToken_isRedacted() {
        logger.info("Authentication token=eyJhbGciOiJIUzI1NiJ9.payload.sig used");

        String output = getCapturedOutput();
        assertThat(output).doesNotContain("eyJhbGciOiJIUzI1NiJ9.payload.sig");
        assertThat(output).contains(REDACTED);
    }

    @Test
    @DisplayName("Secret apiKey=value is redacted in log output")
    void secretApiKey_isRedacted() {
        logger.info("Calling external service with apiKey=sk-abc123xyz789 active");

        String output = getCapturedOutput();
        assertThat(output).doesNotContain("sk-abc123xyz789");
        assertThat(output).contains(REDACTED);
    }

    @Test
    @DisplayName("Multiple secrets in one log line are all redacted")
    void multipleSecrets_allRedacted() {
        logger.info("Credentials: password=p@ss123 token=tkn456 client_secret=cs789");

        String output = getCapturedOutput();
        assertThat(output).doesNotContain("p@ss123");
        assertThat(output).doesNotContain("tkn456");
        assertThat(output).doesNotContain("cs789");
    }

    // ===== PII Masking Tests =====

    @Test
    @DisplayName("Email address is redacted in log output")
    void emailPii_isRedacted() {
        logger.info("Processing request for user@example.com");

        String output = getCapturedOutput();
        assertThat(output).doesNotContain("user@example.com");
        assertThat(output).contains(REDACTED);
    }

    @Test
    @DisplayName("Phone number is redacted in log output")
    void phonePii_isRedacted() {
        logger.info("Contact phone: +1-555-123-4567 stored");

        String output = getCapturedOutput();
        assertThat(output).doesNotContain("555-123-4567");
        assertThat(output).contains(REDACTED);
    }

    @Test
    @DisplayName("Employee ID is redacted in log output")
    void employeeIdPii_isRedacted() {
        logger.info("Employee EMP-12345 submitted feedback");

        String output = getCapturedOutput();
        assertThat(output).doesNotContain("EMP-12345");
        assertThat(output).contains(REDACTED);
    }

    @Test
    @DisplayName("Combined PII and secrets in one message are all redacted")
    void combinedPiiAndSecrets_allRedacted() {
        logger.info("User admin@corp.com logged in with password=SuperSecret123! from +1-800-555-0199");

        String output = getCapturedOutput();
        assertThat(output).doesNotContain("admin@corp.com");
        assertThat(output).doesNotContain("SuperSecret123!");
        assertThat(output).doesNotContain("800-555-0199");
    }

    @Test
    @DisplayName("Non-sensitive messages pass through unchanged")
    void cleanMessage_passesThrough() {
        String cleanMessage = "Processing 42 feedback records from batch job";
        logger.info(cleanMessage);

        String output = getCapturedOutput();
        assertThat(output.trim()).isEqualTo(cleanMessage);
    }
}
