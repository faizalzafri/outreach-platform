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

/**
 * REST controller for managing volunteer feedback operations.
 * Provides endpoints for CRUD, search, export, and metadata queries.
 */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final FeedbackExportService feedbackExportService;

    @Inject
    public FeedbackController(FeedbackService feedbackService, FeedbackExportService feedbackExportService) {
        this.feedbackService = feedbackService;
        this.feedbackExportService = feedbackExportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackDto submitFeedback(@Valid @RequestBody FeedbackSubmitRequest request) {
        return feedbackService.submitFeedback(request);
    }

    @PutMapping("/{eventId}/{employeeId}")
    public FeedbackDto updateFeedback(
            @PathVariable UUID eventId,
            @PathVariable UUID employeeId,
            @Valid @RequestBody FeedbackUpdateRequest request) {
        return feedbackService.updateFeedback(eventId, employeeId, request);
    }

    @GetMapping("/{eventId}/{employeeId}")
    public FeedbackDto getFeedback(@PathVariable UUID eventId, @PathVariable UUID employeeId) {
        return feedbackService.getFeedback(eventId, employeeId);
    }

    @GetMapping("/event/{eventId}")
    public Page<FeedbackDto> listByEvent(@PathVariable UUID eventId, Pageable pageable) {
        return feedbackService.listByEvent(eventId, pageable);
    }

    @GetMapping("/event/{eventId}/status")
    public FeedbackStatusResponse getEventStatus(@PathVariable UUID eventId) {
        return feedbackService.getEventStatus(eventId);
    }

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

    @GetMapping("/export/{eventId}")
    public void exportFeedback(@PathVariable UUID eventId, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"feedback-export-%s.csv\"".formatted(eventId));

        feedbackExportService.exportToCsv(eventId, response.getOutputStream());
    }

    @DeleteMapping("/{eventId}/{employeeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void softDelete(@PathVariable UUID eventId, @PathVariable UUID employeeId) {
        feedbackService.softDelete(eventId, employeeId);
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return feedbackService.getCategories();
    }

    @GetMapping("/tags")
    public List<String> getTags() {
        return feedbackService.getTags();
    }
}
