package com.outreach.platform.ingestion.service;

import com.outreach.platform.ingestion.model.ParsedRow;
import com.outreach.platform.ingestion.model.ValidationError;
import jakarta.inject.Named;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates each parsed row against the volunteer import schema rules.
 */
@Named
public class RowValidator {

    private static final Pattern ALPHANUMERIC = Pattern.compile("^[a-zA-Z0-9]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9\\-\\s]+$");
    private static final Pattern EVENT_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]+$");

    /**
     * Validates a single row against the volunteer import schema rules.
     */
    public List<ValidationError> validate(ParsedRow row) {
        List<ValidationError> errors = new ArrayList<>();
        int rowNum = row.rowNumber();

        // employeeId: required, alphanumeric, max 50 chars
        String employeeId = getField(row, "employeeid");
        if (isBlank(employeeId)) {
            errors.add(new ValidationError(rowNum, "employeeId", "Required field is missing", employeeId));
        } else {
            if (employeeId.length() > 50) {
                errors.add(new ValidationError(rowNum, "employeeId", "Must not exceed 50 characters", employeeId));
            }
            if (!ALPHANUMERIC.matcher(employeeId).matches()) {
                errors.add(new ValidationError(rowNum, "employeeId", "Must be alphanumeric only", employeeId));
            }
        }

        // fullName: required, max 255 chars
        String fullName = getField(row, "fullname");
        if (isBlank(fullName)) {
            errors.add(new ValidationError(rowNum, "fullName", "Required field is missing", fullName));
        } else if (fullName.length() > 255) {
            errors.add(new ValidationError(rowNum, "fullName", "Must not exceed 255 characters", fullName));
        }

        // email: required, valid email format
        String email = getField(row, "email");
        if (isBlank(email)) {
            errors.add(new ValidationError(rowNum, "email", "Required field is missing", email));
        } else if (!EMAIL_PATTERN.matcher(email).matches()) {
            errors.add(new ValidationError(rowNum, "email", "Invalid email format", email));
        }

        // phone: optional, digits/hyphens/spaces only
        String phone = getField(row, "phone");
        if (!isBlank(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add(new ValidationError(rowNum, "phone", "Must contain only digits, hyphens, or spaces", phone));
        }

        // eventCode: required, alphanumeric + hyphens
        String eventCode = getField(row, "eventcode");
        if (isBlank(eventCode)) {
            errors.add(new ValidationError(rowNum, "eventCode", "Required field is missing", eventCode));
        } else if (!EVENT_CODE_PATTERN.matcher(eventCode).matches()) {
            errors.add(new ValidationError(rowNum, "eventCode", "Must be alphanumeric with hyphens only", eventCode));
        }

        return errors;
    }

    /** Validates a date string is in ISO format (yyyy-MM-dd), returning an error if not. */
    public ValidationError validateDate(int rowNumber, String columnName, String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            LocalDate.parse(value);
            return null;
        } catch (DateTimeParseException e) {
            return new ValidationError(rowNumber, columnName,
                    "Date must be in ISO format (yyyy-MM-dd)", value);
        }
    }

    private String getField(ParsedRow row, String key) {
        return row.fields().getOrDefault(key, "");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
