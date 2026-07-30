package com.outreach.platform.common.pii;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as containing PII, triggering encryption at rest and redaction in logs.
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PiiField {

    /** The sensitivity level of the PII data. */
    SensitivityLevel sensitivity() default SensitivityLevel.HIGH;

    /** Optional description for documentation. */
    String description() default "";
}
