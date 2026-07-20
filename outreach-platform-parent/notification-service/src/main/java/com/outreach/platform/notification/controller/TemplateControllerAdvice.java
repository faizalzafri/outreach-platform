package com.outreach.platform.notification.controller;

import com.outreach.platform.notification.service.TemplateService.TemplateNotFoundException;
import com.outreach.platform.notification.service.TemplateService.TemplateVariableValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handling for template-related REST endpoints.
 */
@RestControllerAdvice(assignableTypes = TemplateController.class)
public class TemplateControllerAdvice {

    @ExceptionHandler(TemplateNotFoundException.class)
    public ProblemDetail handleNotFound(TemplateNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Template Not Found");
        return problem;
    }

    @ExceptionHandler(TemplateVariableValidationException.class)
    public ProblemDetail handleValidationError(TemplateVariableValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Template Variable Validation Failed");
        problem.setProperty("errors", ex.getErrors());
        return problem;
    }
}
