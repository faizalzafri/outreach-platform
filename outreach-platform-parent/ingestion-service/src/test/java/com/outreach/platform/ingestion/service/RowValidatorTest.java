package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.model.ParsedRow;
import com.outreach.platform.ingestion.model.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RowValidatorTest {

    private RowValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RowValidator();
    }

    @Test
    void validRow_returnsNoErrors() {
        ParsedRow row = new ParsedRow(2, Map.of(
                "employeeid", "EMP001",
                "fullname", "John Doe",
                "email", "john.doe@example.com",
                "phone", "123-456-7890",
                "eventcode", "EVT-2024-01"
        ));

        List<ValidationError> errors = validator.validate(row);
        assertTrue(errors.isEmpty());
    }

    @Test
    void missingRequiredFields_returnsErrors() {
        ParsedRow row = new ParsedRow(3, Map.of(
                "phone", "123"
        ));

        List<ValidationError> errors = validator.validate(row);
        assertEquals(4, errors.size()); // employeeId, fullName, email, eventCode
    }

    @Test
    void employeeId_nonAlphanumeric_returnsError() {
        ParsedRow row = validRowWith("employeeid", "EMP@001!");

        List<ValidationError> errors = validator.validate(row);
        assertTrue(errors.stream()
                .anyMatch(e -> e.columnName().equals("employeeId")
                        && e.errorMessage().contains("alphanumeric")));
    }

    @Test
    void employeeId_tooLong_returnsError() {
        ParsedRow row = validRowWith("employeeid", "A".repeat(51));

        List<ValidationError> errors = validator.validate(row);
        assertTrue(errors.stream()
                .anyMatch(e -> e.columnName().equals("employeeId")
                        && e.errorMessage().contains("50 characters")));
    }

    @Test
    void email_invalidFormat_returnsError() {
        ParsedRow row = validRowWith("email", "not-an-email");

        List<ValidationError> errors = validator.validate(row);
        assertTrue(errors.stream()
                .anyMatch(e -> e.columnName().equals("email")
                        && e.errorMessage().contains("Invalid email")));
    }

    @Test
    void phone_invalidChars_returnsError() {
        ParsedRow row = validRowWith("phone", "+1(555)123");

        List<ValidationError> errors = validator.validate(row);
        assertTrue(errors.stream()
                .anyMatch(e -> e.columnName().equals("phone")
                        && e.errorMessage().contains("digits, hyphens, or spaces")));
    }

    @Test
    void phone_optional_blank_noError() {
        ParsedRow row = new ParsedRow(2, Map.of(
                "employeeid", "EMP001",
                "fullname", "John Doe",
                "email", "john@example.com",
                "phone", "",
                "eventcode", "EVT01"
        ));

        List<ValidationError> errors = validator.validate(row);
        assertTrue(errors.isEmpty());
    }

    @Test
    void eventCode_invalidChars_returnsError() {
        ParsedRow row = validRowWith("eventcode", "EVT_2024@01");

        List<ValidationError> errors = validator.validate(row);
        assertTrue(errors.stream()
                .anyMatch(e -> e.columnName().equals("eventCode")
                        && e.errorMessage().contains("alphanumeric with hyphens")));
    }

    @Test
    void validateDate_validIsoDate_returnsNull() {
        assertNull(validator.validateDate(2, "eventDate", "2024-01-15"));
    }

    @Test
    void validateDate_invalidDate_returnsError() {
        ValidationError error = validator.validateDate(2, "eventDate", "15/01/2024");
        assertNotNull(error);
        assertEquals("eventDate", error.columnName());
        assertTrue(error.errorMessage().contains("ISO format"));
    }

    @Test
    void validateDate_blank_returnsNull() {
        assertNull(validator.validateDate(2, "eventDate", ""));
    }

    private ParsedRow validRowWith(String field, String value) {
        Map<String, String> fields = new java.util.LinkedHashMap<>(Map.of(
                "employeeid", "EMP001",
                "fullname", "John Doe",
                "email", "john@example.com",
                "phone", "123-456-7890",
                "eventcode", "EVT01"
        ));
        fields.put(field, value);
        return new ParsedRow(2, fields);
    }
}
