package com.outreach.platform.event.controller;

import com.outreach.platform.event.model.dto.BeneficiaryCreateRequest;
import com.outreach.platform.event.model.dto.BeneficiaryDto;
import com.outreach.platform.event.model.dto.BeneficiaryUpdateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.service.BeneficiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST controller for beneficiary organizations: create, list/search, view, update, and linked events. */
@RestController
@RequestMapping("/beneficiaries")
@Tag(name = "Beneficiaries", description = "Beneficiary organizations served by outreach events")
@PreAuthorize("hasAnyRole('PMO', 'ADMIN', 'TENANT_ADMIN', 'PLATFORM_ADMIN')")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @Inject
    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @Operation(summary = "List beneficiaries", description = "Lists beneficiaries, optionally searching by name, organization, or city")
    @GetMapping
    public Page<BeneficiaryDto> listBeneficiaries(
            @Parameter(description = "Free-text search across name, organization, and city")
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return search == null || search.isBlank()
                ? beneficiaryService.listBeneficiaries(pageable)
                : beneficiaryService.searchBeneficiaries(search, pageable);
    }

    @Operation(summary = "Create beneficiary")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BeneficiaryDto createBeneficiary(@Valid @RequestBody BeneficiaryCreateRequest request) {
        return beneficiaryService.createBeneficiary(request);
    }

    @Operation(summary = "Get beneficiary")
    @GetMapping("/{id}")
    public BeneficiaryDto getBeneficiary(@PathVariable UUID id) {
        return beneficiaryService.getBeneficiary(id);
    }

    @Operation(summary = "Update beneficiary", description = "Updates the provided fields; omitted fields are left unchanged")
    @PutMapping("/{id}")
    public BeneficiaryDto updateBeneficiary(@PathVariable UUID id,
                                            @Valid @RequestBody BeneficiaryUpdateRequest request) {
        return beneficiaryService.updateBeneficiary(id, request);
    }

    @Operation(summary = "List beneficiary's events")
    @GetMapping("/{id}/events")
    public List<EventDto> getEventsForBeneficiary(@PathVariable UUID id) {
        return beneficiaryService.getEventsForBeneficiary(id);
    }
}
