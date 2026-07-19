package com.outreach.platform.common.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for GlobalExceptionHandler.
 * Validates that input validation errors produce structured ErrorResponse.
 */
@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class GlobalExceptionHandlerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST with invalid body returns 400 with structured ErrorResponse and fieldErrors")
    void invalidRequest_returnsStructuredErrorResponse() throws Exception {
        // Send a request with blank name and invalid email
        String invalidJson = """
                {
                    "name": "",
                    "email": "not-an-email"
                }
                """;

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andExpect(jsonPath("$.fieldErrors.name").isArray())
                .andExpect(jsonPath("$.fieldErrors.email").isArray());
    }

    @Test
    @DisplayName("POST with missing required fields returns 400 with all field errors listed")
    void missingFields_returnsAllFieldErrors() throws Exception {
        String emptyJson = "{}";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.name", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.fieldErrors.email", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("POST with malformed JSON returns 400 with MALFORMED_REQUEST error")
    void malformedJson_returnsMalformedRequestError() throws Exception {
        String malformedJson = "{ this is not json }";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is malformed or contains unrecognized fields"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST with valid data returns 200 OK")
    void validRequest_returns200() throws Exception {
        String validJson = """
                {
                    "name": "John Doe",
                    "email": "john@example.com"
                }
                """;

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Correlation ID is present in error response even without header")
    void errorResponse_alwaysHasCorrelationId() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "email": ""
                }
                """;

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }
}
