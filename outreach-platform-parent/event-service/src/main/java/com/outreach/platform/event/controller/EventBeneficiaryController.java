package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.BeneficiaryDto;
import com.outreach.platform.event.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** The beneficiaries an event serves. */
@RestController
@RequestMapping("/events/{eventId}/beneficiaries")
@Tag(name = "Event Beneficiaries", description = "Link beneficiaries to an event")
@PreAuthorize("hasAnyRole('PMO', 'ADMIN', 'TENANT_ADMIN', 'PLATFORM_ADMIN')")
public class EventBeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @Inject
    public EventBeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @Operation(summary = "List an event's beneficiaries")
    @GetMapping
    public List<BeneficiaryDto> list(@PathVariable UUID eventId) {
        return beneficiaryService.getBeneficiariesForEvent(eventId);
    }

    @Operation(summary = "Link a beneficiary", description = "Idempotent; returns the event's beneficiaries")
    @PutMapping("/{beneficiaryId}")
    public List<BeneficiaryDto> link(@PathVariable UUID eventId, @PathVariable UUID beneficiaryId) {
        return beneficiaryService.linkToEvent(eventId, beneficiaryId);
    }

    @Operation(summary = "Unlink a beneficiary")
    @DeleteMapping("/{beneficiaryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@PathVariable UUID eventId, @PathVariable UUID beneficiaryId) {
        beneficiaryService.unlinkFromEvent(eventId, beneficiaryId);
    }
}
