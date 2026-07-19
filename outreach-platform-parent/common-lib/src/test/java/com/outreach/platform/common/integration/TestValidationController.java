package com.outreach.platform.common.integration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal test controller used for integration testing of GlobalExceptionHandler.
 * Accepts a validated DTO to trigger validation error responses.
 */
@RestController
@RequestMapping("/test")
public class TestValidationController {

    @PostMapping("/validate")
    public ResponseEntity<String> validateInput(@Valid @RequestBody TestDto dto) {
        return ResponseEntity.ok("Valid: " + dto.name());
    }

    /**
     * Simple DTO with Bean Validation constraints for testing.
     */
    public record TestDto(
            @NotBlank(message = "name must not be blank")
            @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
            String name,

            @NotBlank(message = "email must not be blank")
            @Email(message = "email must be a valid email address")
            String email
    ) {}
}
