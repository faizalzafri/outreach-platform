package com.outreach.platform.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Validates provided template variables against the declared JSON schema. */
@Service
public class TemplateValidationService {

    private final ObjectMapper objectMapper;

    @Inject
    public TemplateValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates that the provided variables satisfy the template's schema, returning any errors.
     *
     * <p>{@code variablesSchema} is a standard JSON Schema object — {@code {"type":"object",
     * "properties":{"varName":{"type":"string"}, ...},"required":["varName"]}} — matching every
     * seeded template and this DTO's own Swagger examples. required lives as a top-level array of
     * variable names, per the JSON Schema spec, not nested per-property.
     */
    public List<String> validate(String variablesSchema, Map<String, Object> variables) {
        List<String> errors = new ArrayList<>();

        if (variablesSchema == null || variablesSchema.isBlank()) {
            return errors;
        }

        Map<String, Object> schema;
        try {
            schema = objectMapper.readValue(variablesSchema, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            errors.add("Invalid variables_schema JSON: " + e.getMessage());
            return errors;
        }

        Set<String> providedKeys = (variables != null) ? variables.keySet() : Set.of();

        Set<String> requiredVars = (schema.get("required") instanceof List<?> required)
                ? required.stream().map(String::valueOf).collect(Collectors.toSet())
                : Set.of();

        for (String varName : requiredVars) {
            if (!providedKeys.contains(varName)) {
                errors.add("Missing required variable: " + varName);
            }
        }

        return errors;
    }
}
