package com.outreach.platform.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for TemplateValidationService — validates that provided
 * variables satisfy the template's declared schema.
 */
class TemplateValidationServiceTest {

    private TemplateValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new TemplateValidationService(new ObjectMapper());
    }

    @Test
    void validWhenAllRequiredVariablesPresent() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "volunteerName": { "type": "string" },
                    "eventDate": { "type": "string" }
                  },
                  "required": ["volunteerName", "eventDate"]
                }
                """;
        Map<String, Object> variables = Map.of(
                "volunteerName", "Alice",
                "eventDate", "2025-03-15"
        );

        List<String> errors = validationService.validate(schema, variables);

        assertTrue(errors.isEmpty());
    }

    @Test
    void reportsErrorWhenRequiredVariableMissing() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "volunteerName": { "type": "string" },
                    "eventDate": { "type": "string" }
                  },
                  "required": ["volunteerName", "eventDate"]
                }
                """;
        Map<String, Object> variables = Map.of("volunteerName", "Alice");

        List<String> errors = validationService.validate(schema, variables);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("eventDate"));
    }

    @Test
    void allowsOptionalVariablesToBeMissing() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "volunteerName": { "type": "string" },
                    "notes": { "type": "string" }
                  },
                  "required": ["volunteerName"]
                }
                """;
        Map<String, Object> variables = Map.of("volunteerName", "Bob");

        List<String> errors = validationService.validate(schema, variables);

        assertTrue(errors.isEmpty());
    }

    @Test
    void validWhenSchemaIsNull() {
        List<String> errors = validationService.validate(null, Map.of("key", "value"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void validWhenSchemaIsBlank() {
        List<String> errors = validationService.validate("  ", Map.of("key", "value"));
        assertTrue(errors.isEmpty());
    }

    @Test
    void reportsErrorForInvalidSchemaJson() {
        String invalidJson = "{ not valid json }";

        List<String> errors = validationService.validate(invalidJson, Map.of());

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Invalid variables_schema JSON"));
    }

    @Test
    void reportsMultipleMissingRequiredVariables() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" },
                    "email": { "type": "string" },
                    "city": { "type": "string" }
                  },
                  "required": ["name", "email", "city"]
                }
                """;
        Map<String, Object> variables = Map.of("city", "Mumbai");

        List<String> errors = validationService.validate(schema, variables);

        assertEquals(2, errors.size());
    }
}
