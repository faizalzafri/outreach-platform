package com.outreach.platform.event.controller;

import com.outreach.platform.event.service.TeamService.DuplicateTeamMemberException;
import com.outreach.platform.event.service.TeamService.DuplicateTeamNameException;
import com.outreach.platform.event.service.TeamService.MemberNotFoundException;
import com.outreach.platform.event.service.TeamService.TeamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Exception handler for team management endpoints using RFC 9457 Problem Detail. */
@RestControllerAdvice(assignableTypes = TeamController.class)
public class TeamControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(TeamControllerAdvice.class);

    @ExceptionHandler(TeamNotFoundException.class)
    public ProblemDetail handleTeamNotFound(TeamNotFoundException ex) {
        log.warn("Team not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Team Not Found");
        problem.setProperty("error", "TEAM_NOT_FOUND");
        return problem;
    }

    @ExceptionHandler(DuplicateTeamNameException.class)
    public ProblemDetail handleDuplicateTeamName(DuplicateTeamNameException ex) {
        log.warn("Duplicate team name: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Team Name");
        problem.setProperty("error", "DUPLICATE_TEAM_NAME");
        return problem;
    }

    @ExceptionHandler(DuplicateTeamMemberException.class)
    public ProblemDetail handleDuplicateTeamMember(DuplicateTeamMemberException ex) {
        log.warn("Duplicate team member: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Team Member");
        problem.setProperty("error", "DUPLICATE_TEAM_MEMBER");
        return problem;
    }

    @ExceptionHandler(MemberNotFoundException.class)
    public ProblemDetail handleMemberNotFound(MemberNotFoundException ex) {
        log.warn("Team member not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Team Member Not Found");
        problem.setProperty("error", "TEAM_MEMBER_NOT_FOUND");
        return problem;
    }
}
