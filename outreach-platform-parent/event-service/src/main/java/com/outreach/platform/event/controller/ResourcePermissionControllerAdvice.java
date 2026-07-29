package com.outreach.platform.event.controller;

import com.outreach.platform.event.service.PermissionService.AccessDeniedException;
import com.outreach.platform.event.service.PermissionService.NoTeamGrantedException;
import com.outreach.platform.event.service.PermissionService.PermissionNotFoundException;
import com.outreach.platform.event.service.PermissionService.ResourceNotFoundException;
import com.outreach.platform.event.service.PermissionService.TeamMembershipRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Exception handling for resource permission REST endpoints.
 * Uses RFC 9457 Problem Detail responses.
 */
@RestControllerAdvice(assignableTypes = ResourcePermissionController.class)
public class ResourcePermissionControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ResourcePermissionControllerAdvice.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setProperty("error", "RESOURCE_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(PermissionNotFoundException.class)
    public ProblemDetail handlePermissionNotFound(PermissionNotFoundException ex) {
        log.warn("Permission not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Permission Not Found");
        problem.setProperty("error", "PERMISSION_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access Denied");
        problem.setProperty("error", "ACCESS_DENIED");
        return problem;
    }

    @ExceptionHandler(TeamMembershipRequiredException.class)
    public ProblemDetail handleTeamMembershipRequired(TeamMembershipRequiredException ex) {
        log.warn("Team membership required: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Team Membership Required");
        problem.setProperty("error", "TEAM_MEMBERSHIP_REQUIRED");
        return problem;
    }

    @ExceptionHandler(NoTeamGrantedException.class)
    public ProblemDetail handleNoTeamGranted(NoTeamGrantedException ex) {
        log.warn("No team granted: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("No Team Granted");
        problem.setProperty("error", "NO_TEAM_GRANTED");
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        String detail = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid Parameter");
        problem.setProperty("error", "INVALID_PARAMETER");
        return problem;
    }
}
