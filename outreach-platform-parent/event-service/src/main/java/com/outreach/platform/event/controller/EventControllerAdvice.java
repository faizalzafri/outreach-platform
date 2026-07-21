package com.outreach.platform.event.controller;

import com.outreach.platform.event.service.EventService.EventNotFoundException;
import com.outreach.platform.event.service.EventService.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handling for event lifecycle REST endpoints.
 */
@RestControllerAdvice(assignableTypes = EventController.class)
public class EventControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(EventControllerAdvice.class);

    @ExceptionHandler(EventNotFoundException.class)
    public ProblemDetail handleNotFound(EventNotFoundException ex) {
        log.warn("Event not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Event Not Found");
        return problem;
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Status Transition");
        problem.setProperty("fromStatus", ex.getFrom().name());
        problem.setProperty("toStatus", ex.getTo().name());
        return problem;
    }
}
