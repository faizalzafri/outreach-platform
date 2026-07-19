package com.outreach.platform.common.pii;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as containing Personally Identifiable Information (PII).
 * Fields annotated with @PiiField will be:
 * - Encrypted at rest using AES-256
 * - Redacted in log output
 * - Subject to CI/CD audit rules
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PiiField {

    /**
     * The sensitivity level of the PII data.
     */
    SensitivityLevel sensitivity() default SensitivityLevel.HIGH;

    /**
     * Optional description of the PII field for documentation.
     */
    String description() default "";
}
