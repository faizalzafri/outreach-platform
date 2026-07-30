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

/** Validates provided template variables against the declared JSON schema. */
@Service
public class TemplateValidationService {

    private final ObjectMapper objectMapper;

    @Inject
    public TemplateValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Validates that the provided variables satisfy the template's schema, returning any errors. */
    public List<String> validate(String variablesSchema, Map<String, Object> variables) {
        List<String> errors = new ArrayList<>();

        if (variablesSchema == null || variablesSchema.isBlank()) {
            return errors;
        }

        Map<String, Map<String, Object>> schema;
        try {
            schema = objectMapper.readValue(variablesSchema, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            errors.add("Invalid variables_schema JSON: " + e.getMessage());
            return errors;
        }

        Set<String> providedKeys = (variables != null) ? variables.keySet() : Set.of();

        for (Map.Entry<String, Map<String, Object>> entry : schema.entrySet()) {
            String varName = entry.getKey();
            Map<String, Object> varDef = entry.getValue();

            boolean required = Boolean.TRUE.equals(varDef.get("required"));
            if (required && !providedKeys.contains(varName)) {
                errors.add("Missing required variable: " + varName);
            }
        }

        return errors;
    }
}
