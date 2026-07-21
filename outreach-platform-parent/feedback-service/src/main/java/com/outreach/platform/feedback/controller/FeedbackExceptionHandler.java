package com.outreach.platform.feedback.controller;

import com.outreach.platform.feedback.service.FeedbackAlreadyExistsException;
import com.outreach.platform.feedback.service.FeedbackConflictException;
import com.outreach.platform.feedback.service.FeedbackNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception handling for feedback REST endpoints.
 * Maps domain exceptions to appropriate HTTP responses using RFC 7807 Problem Detail.
 */
@RestControllerAdvice
public class FeedbackExceptionHandler {

    @ExceptionHandler(FeedbackNotFoundException.class)
    public ProblemDetail handleNotFound(FeedbackNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Feedback Not Found");
        return problem;
    }

    @ExceptionHandler(FeedbackAlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(FeedbackAlreadyExistsException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Feedback Already Exists");
        return problem;
    }

    @ExceptionHandler(FeedbackConflictException.class)
    public ProblemDetail handleConflict(FeedbackConflictException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Optimistic Lock Conflict");
        return problem;
    }
}
