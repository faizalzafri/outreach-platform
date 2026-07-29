package com.outreach.platform.event.controller;

import com.outreach.platform.event.service.TenantService.DuplicateTenantNameException;
import com.outreach.platform.event.service.TenantService.DuplicateTenantSlugException;
import com.outreach.platform.event.service.TenantService.TenantNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handling for tenant management REST endpoints.
 * Uses RFC 9457 Problem Detail responses.
 */
@RestControllerAdvice(assignableTypes = TenantController.class)
public class TenantControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(TenantControllerAdvice.class);

    @ExceptionHandler(TenantNotFoundException.class)
    public ProblemDetail handleTenantNotFound(TenantNotFoundException ex) {
        log.warn("Tenant not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Tenant Not Found");
        problem.setProperty("error", "TENANT_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(DuplicateTenantNameException.class)
    public ProblemDetail handleDuplicateName(DuplicateTenantNameException ex) {
        log.warn("Duplicate tenant name: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Tenant Name");
        problem.setProperty("error", "DUPLICATE_TENANT_NAME");
        return problem;
    }

    @ExceptionHandler(DuplicateTenantSlugException.class)
    public ProblemDetail handleDuplicateSlug(DuplicateTenantSlugException ex) {
        log.warn("Duplicate tenant slug: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Tenant Slug");
        problem.setProperty("error", "DUPLICATE_TENANT_SLUG");
        return problem;
    }
}
