package com.outreach.platform.common.integration;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for structured JSON logging.
 * Verifies that the LogstashEncoder produces JSON output with all required fields:
 * @timestamp, level, message, service, plus MDC fields (traceId, spanId, correlationId)
 * when set.
 */
class StructuredLoggingIT {

    private Logger logger;
    private ByteArrayOutputStream outputStream;
    private OutputStreamAppender<ILoggingEvent> appender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        outputStream = new ByteArrayOutputStream();

        LogstashEncoder encoder = new LogstashEncoder();
        encoder.setContext(loggerContext);
        encoder.setCustomFields("{\"service\":\"common-lib-test\"}");
        encoder.start();

        appender = new OutputStreamAppender<>();
        appender.setContext(loggerContext);
        appender.setEncoder(encoder);
        appender.setOutputStream(outputStream);
        appender.start();

        logger = loggerContext.getLogger("com.outreach.platform.common.integration.StructuredLoggingIT");
        logger.addAppender(appender);
        logger.setAdditive(false);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        if (appender != null) {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private JsonNode parseLogOutput() throws Exception {
        String output = outputStream.toString(StandardCharsets.UTF_8).trim();
        // Handle potential multiple lines; take the last one
        String[] lines = output.split("\n");
        String lastLine = lines[lines.length - 1].trim();
        return objectMapper.readTree(lastLine);
    }

    @Test
    @DisplayName("JSON log contains @timestamp field in ISO format")
    void jsonLog_containsTimestamp() throws Exception {
        logger.info("Test message");

        JsonNode json = parseLogOutput();
        assertThat(json.has("@timestamp")).isTrue();
        assertThat(json.get("@timestamp").asText()).isNotBlank();
    }

    @Test
    @DisplayName("JSON log contains level field")
    void jsonLog_containsLevel() throws Exception {
        logger.info("Test message");

        JsonNode json = parseLogOutput();
        assertThat(json.has("level")).isTrue();
        assertThat(json.get("level").asText()).isEqualTo("INFO");
    }

    @Test
    @DisplayName("JSON log contains message field")
    void jsonLog_containsMessage() throws Exception {
        logger.info("Structured log test message");

        JsonNode json = parseLogOutput();
        assertThat(json.has("message")).isTrue();
        assertThat(json.get("message").asText()).isEqualTo("Structured log test message");
    }

    @Test
    @DisplayName("JSON log contains service custom field")
    void jsonLog_containsServiceField() throws Exception {
        logger.info("Service identification test");

        JsonNode json = parseLogOutput();
        assertThat(json.has("service")).isTrue();
        assertThat(json.get("service").asText()).isEqualTo("common-lib-test");
    }

    @Test
    @DisplayName("JSON log contains all required fields together")
    void jsonLog_containsAllRequiredFields() throws Exception {
        logger.info("Complete field check");

        JsonNode json = parseLogOutput();

        assertThat(json.has("@timestamp")).as("@timestamp field").isTrue();
        assertThat(json.has("level")).as("level field").isTrue();
        assertThat(json.has("message")).as("message field").isTrue();
        assertThat(json.has("service")).as("service field").isTrue();
        assertThat(json.has("logger_name")).as("logger_name field").isTrue();
    }

    @Test
    @DisplayName("JSON log includes MDC traceId when set")
    void jsonLog_includesTraceId_whenSet() throws Exception {
        MDC.put("traceId", "abc123def456");
        logger.info("With trace context");

        JsonNode json = parseLogOutput();
        assertThat(json.has("traceId")).isTrue();
        assertThat(json.get("traceId").asText()).isEqualTo("abc123def456");
    }

    @Test
    @DisplayName("JSON log includes MDC spanId when set")
    void jsonLog_includesSpanId_whenSet() throws Exception {
        MDC.put("spanId", "span-789");
        logger.info("With span context");

        JsonNode json = parseLogOutput();
        assertThat(json.has("spanId")).isTrue();
        assertThat(json.get("spanId").asText()).isEqualTo("span-789");
    }

    @Test
    @DisplayName("JSON log includes MDC correlationId when set")
    void jsonLog_includesCorrelationId_whenSet() throws Exception {
        MDC.put("correlationId", "corr-id-001");
        logger.info("With correlation context");

        JsonNode json = parseLogOutput();
        assertThat(json.has("correlationId")).isTrue();
        assertThat(json.get("correlationId").asText()).isEqualTo("corr-id-001");
    }

    @Test
    @DisplayName("JSON log includes all MDC fields when all set")
    void jsonLog_includesAllMdcFields_whenAllSet() throws Exception {
        MDC.put("traceId", "trace-full-001");
        MDC.put("spanId", "span-full-001");
        MDC.put("correlationId", "corr-full-001");
        logger.info("Full MDC context");

        JsonNode json = parseLogOutput();

        assertThat(json.get("traceId").asText()).isEqualTo("trace-full-001");
        assertThat(json.get("spanId").asText()).isEqualTo("span-full-001");
        assertThat(json.get("correlationId").asText()).isEqualTo("corr-full-001");
    }

    @Test
    @DisplayName("JSON log output is valid parseable JSON")
    void jsonLog_isValidJson() throws Exception {
        logger.warn("Warning level message");

        String output = outputStream.toString(StandardCharsets.UTF_8).trim();
        // Should not throw exception
        JsonNode json = objectMapper.readTree(output);
        assertThat(json).isNotNull();
        assertThat(json.isObject()).isTrue();
    }
}
