package com.outreach.platform.event.controller;

import com.outreach.platform.event.service.TenantMembershipService.DuplicateMembershipException;
import com.outreach.platform.event.service.TenantMembershipService.LastAdminRemovalException;
import com.outreach.platform.event.service.TenantMembershipService.MemberNotFoundException;
import com.outreach.platform.event.service.TenantMembershipService.SelfElevationForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handling for tenant membership REST endpoints.
 */
@RestControllerAdvice(assignableTypes = TenantMembershipController.class)
public class TenantMembershipControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(TenantMembershipControllerAdvice.class);

    @ExceptionHandler(DuplicateMembershipException.class)
    public ProblemDetail handleDuplicateMembership(DuplicateMembershipException ex) {
        log.warn("Duplicate membership: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Membership");
        problem.setProperty("error", "DUPLICATE_MEMBERSHIP");
        return problem;
    }

    @ExceptionHandler(LastAdminRemovalException.class)
    public ProblemDetail handleLastAdminRemoval(LastAdminRemovalException ex) {
        log.warn("Last admin removal attempt: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Last Admin Removal");
        problem.setProperty("error", "LAST_ADMIN_REMOVAL");
        return problem;
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ProblemDetail handleMemberNotFound(MemberNotFoundException ex) {
        log.warn("Member not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Member Not Found");
        problem.setProperty("error", "MEMBER_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Cross-Tenant Forbidden");
        problem.setProperty("error", "CROSS_TENANT_FORBIDDEN");
        return problem;
    }

    @ExceptionHandler(SelfElevationForbiddenException.class)
    public ProblemDetail handleSelfElevationForbidden(SelfElevationForbiddenException ex) {
        log.warn("Self-elevation attempt blocked: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Self-Elevation Forbidden");
        problem.setProperty("error", "SELF_ELEVATION_FORBIDDEN");
        return problem;
    }
}
