package com.outreach.platform.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Global ObjectMapper configuration shared across all platform services.
 * Enforces consistent serialization: alphabetical properties, ISO-8601 dates,
 * uppercase enum strings, and strict deserialization that rejects unknown fields.
 */
@Configuration
public class JacksonConfig {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JsonMapper.builder()
                // Alphabetical property serialization
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                // Reject unknown JSON properties on deserialization
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // ISO-8601 dates (disable writing dates as timestamps)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Serialize enums using toString (uppercase)
                .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
                .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .build()
                // Register JavaTimeModule for Java 8+ date/time types
                .registerModule(new JavaTimeModule());
    }
}
