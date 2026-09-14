package com.outreach.platform.auth.controller;

import com.outreach.platform.auth.service.TenantMembershipService.MembershipNotFoundException;
import com.outreach.platform.auth.service.TenantMembershipService.TenantNotActiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Exception handler for tenant selection endpoints using RFC 9457 Problem Detail. */
@RestControllerAdvice(assignableTypes = TenantSelectionController.class)
public class TenantSelectionControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(TenantSelectionControllerAdvice.class);

    @ExceptionHandler(MembershipNotFoundException.class)
    public ProblemDetail handleMembershipNotFound(MembershipNotFoundException ex) {
        log.warn("Tenant membership not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Tenant Membership Not Found");
        problem.setProperty("error", "TENANT_MEMBERSHIP_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(TenantNotActiveException.class)
    public ProblemDetail handleTenantNotActive(TenantNotActiveException ex) {
        log.warn("Cannot select inactive tenant: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Tenant Not Active");
        problem.setProperty("error", "TENANT_NOT_ACTIVE");
        return problem;
    }
}
