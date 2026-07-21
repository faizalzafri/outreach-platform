package com.outreach.platform.event.controller;

import com.outreach.platform.event.service.AdminService.InvalidRoleException;
import com.outreach.platform.event.service.AdminService.UserNotFoundException;
import com.outreach.platform.event.service.AdminService.UsernameAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handling for admin REST endpoints.
 */
@RestControllerAdvice(assignableTypes = AdminController.class)
public class AdminControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(AdminControllerAdvice.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("User Not Found");
        return problem;
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ProblemDetail handleUsernameConflict(UsernameAlreadyExistsException ex) {
        log.warn("Username conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Username Already Exists");
        return problem;
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ProblemDetail handleInvalidRole(InvalidRoleException ex) {
        log.warn("Invalid role: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Role");
        return problem;
    }
}
