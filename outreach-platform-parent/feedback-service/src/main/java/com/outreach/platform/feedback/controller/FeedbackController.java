package com.outreach.platform.feedback.controller;

import com.outreach.platform.feedback.model.FeedbackSentiment;
import com.outreach.platform.feedback.model.FeedbackStatus;
import com.outreach.platform.feedback.model.dto.FeedbackDto;
import com.outreach.platform.feedback.model.dto.FeedbackSearchRequest;
import com.outreach.platform.feedback.model.dto.FeedbackStatusResponse;
import com.outreach.platform.feedback.model.dto.FeedbackSubmitRequest;
import com.outreach.platform.feedback.model.dto.FeedbackUpdateRequest;
import com.outreach.platform.feedback.service.FeedbackExportService;
import com.outreach.platform.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** REST controller for volunteer feedback CRUD, search, and export operations. */
@RestController
@RequestMapping("/feedback")
@Tag(name = "Feedback Management", description = "Volunteer feedback submission, search, export, and metadata operations")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final FeedbackExportService feedbackExportService;

    @Inject
    public FeedbackController(FeedbackService feedbackService, FeedbackExportService feedbackExportService) {
        this.feedbackService = feedbackService;
        this.feedbackExportService = feedbackExportService;
    }

    @Operation(summary = "Submit feedback", description = "Submits new volunteer feedback for an event")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackDto submitFeedback(@Valid @RequestBody FeedbackSubmitRequest request) {
        return feedbackService.submitFeedback(request);
    }

    @Operation(summary = "Update feedback", description = "Updates an existing feedback record by event and employee")
    @PutMapping("/{eventId}/{employeeId}")
    public FeedbackDto updateFeedback(
            @PathVariable UUID eventId,
            @PathVariable UUID employeeId,
            @Valid @RequestBody FeedbackUpdateRequest request) {
        return feedbackService.updateFeedback(eventId, employeeId, request);
    }

    @Operation(summary = "Get feedback", description = "Retrieves a feedback record by event and employee composite key")
    @GetMapping("/{eventId}/{employeeId}")
    public FeedbackDto getFeedback(@Parameter(description = "Event UUID") @PathVariable UUID eventId,
                                   @Parameter(description = "Employee UUID") @PathVariable UUID employeeId) {
        return feedbackService.getFeedback(eventId, employeeId);
    }

    @Operation(summary = "List by event", description = "Lists all feedback for an event with pagination")
    @GetMapping("/event/{eventId}")
    public Page<FeedbackDto> listByEvent(@Parameter(description = "Event UUID") @PathVariable UUID eventId, Pageable pageable) {
        return feedbackService.listByEvent(eventId, pageable);
    }

    @Operation(summary = "Event feedback status", description = "Returns feedback completion statistics for an event")
    @GetMapping("/event/{eventId}/status")
    public FeedbackStatusResponse getEventStatus(@Parameter(description = "Event UUID") @PathVariable UUID eventId) {
        return feedbackService.getEventStatus(eventId);
    }

    @Operation(summary = "Search feedback", description = "Searches feedback with filters: event, employee, category, sentiment, score range, and date range")
    @GetMapping("/search")
    public Page<FeedbackDto> search(
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) FeedbackSentiment sentiment,
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(required = false) Instant dateFrom,
            @RequestParam(required = false) Instant dateTo,
            Pageable pageable) {

        FeedbackSearchRequest searchRequest = new FeedbackSearchRequest(
                eventId, employeeId, category, sentiment, status, minScore, maxScore, dateFrom, dateTo
        );
        return feedbackService.search(searchRequest, pageable);
    }

    @Operation(summary = "Export feedback", description = "Exports feedback for an event as CSV")
    @GetMapping("/export/{eventId}")
    public void exportFeedback(@Parameter(description = "Event UUID") @PathVariable UUID eventId, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"feedback-export-%s.csv\"".formatted(eventId));

        feedbackExportService.exportToCsv(eventId, response.getOutputStream());
    }

    @Operation(summary = "Soft delete feedback", description = "Soft-deletes a feedback record")
    @DeleteMapping("/{eventId}/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@PathVariable UUID eventId, @PathVariable UUID employeeId) {
        feedbackService.softDelete(eventId, employeeId);
    }

    @Operation(summary = "List categories", description = "Returns all feedback categories")
    @GetMapping("/categories")
    public List<String> getCategories() {
        return feedbackService.getCategories();
    }

    @Operation(summary = "List tags", description = "Returns all feedback tags")
    @GetMapping("/tags")
    public List<String> getTags() {
        return feedbackService.getTags();
    }
}
