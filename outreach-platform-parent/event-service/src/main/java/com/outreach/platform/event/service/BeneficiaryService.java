package com.outreach.platform.event.service;

import com.outreach.platform.common.tenant.TenantContext;
import com.outreach.platform.event.entity.BeneficiaryEntity;
import com.outreach.platform.event.entity.EventBeneficiaryEntity;
import com.outreach.platform.event.mapper.BeneficiaryMapper;
import com.outreach.platform.event.model.dto.BeneficiaryCreateRequest;
import com.outreach.platform.event.model.dto.BeneficiaryDto;
import com.outreach.platform.event.model.dto.BeneficiaryUpdateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.mapper.EventMapper;
import com.outreach.platform.event.repo.BeneficiaryRepository;
import com.outreach.platform.event.repo.EventBeneficiaryRepository;
import jakarta.inject.Inject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Business logic for beneficiary management.
 */
@Service
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final EventBeneficiaryRepository eventBeneficiaryRepository;
    private final BeneficiaryMapper beneficiaryMapper;
    private final EventMapper eventMapper;

    @Inject
    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository,
                              EventBeneficiaryRepository eventBeneficiaryRepository,
                              BeneficiaryMapper beneficiaryMapper,
                              EventMapper eventMapper) {
        this.beneficiaryRepository = beneficiaryRepository;
        this.eventBeneficiaryRepository = eventBeneficiaryRepository;
        this.beneficiaryMapper = beneficiaryMapper;
        this.eventMapper = eventMapper;
    }

    /**
     * Creates a new beneficiary.
     */
    @Transactional
    public BeneficiaryDto createBeneficiary(BeneficiaryCreateRequest request) {
        BeneficiaryEntity entity = new BeneficiaryEntity();
        entity.setName(request.name());
        entity.setOrganization(request.organization());
        entity.setContactEmail(request.contactEmail());
        entity.setContactPhone(request.contactPhone());
        entity.setCity(request.city());
        entity.setAddress(request.address());
        entity.setDescription(request.description());
        entity.setActive(true);

        BeneficiaryEntity saved = beneficiaryRepository.save(entity);
        return beneficiaryMapper.toDto(saved);
    }

    /**
     * Lists beneficiaries with pagination.
     */
    @Transactional(readOnly = true)
    public Page<BeneficiaryDto> listBeneficiaries(Pageable pageable) {
        return beneficiaryRepository.findAll(pageable)
                .map(beneficiaryMapper::toDto);
    }

    /**
     * Gets beneficiary details by ID.
     *
     * @throws NoSuchElementException if not found
     */
    @Transactional(readOnly = true)
    public BeneficiaryDto getBeneficiary(UUID id) {
        BeneficiaryEntity entity = findOrThrow(id);
        return beneficiaryMapper.toDto(entity);
    }

    /**
     * Updates a beneficiary's fields.
     *
     * @throws NoSuchElementException if not found
     */
    @Transactional
    public BeneficiaryDto updateBeneficiary(UUID id, BeneficiaryUpdateRequest request) {
        BeneficiaryEntity entity = findOrThrow(id);

        if (request.name() != null) entity.setName(request.name());
        if (request.organization() != null) entity.setOrganization(request.organization());
        if (request.contactEmail() != null) entity.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) entity.setContactPhone(request.contactPhone());
        if (request.city() != null) entity.setCity(request.city());
        if (request.address() != null) entity.setAddress(request.address());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.active() != null) entity.setActive(request.active());

        BeneficiaryEntity saved = beneficiaryRepository.save(entity);
        return beneficiaryMapper.toDto(saved);
    }

    /**
     * Lists events associated with a beneficiary.
     */
    @Transactional(readOnly = true)
    public List<EventDto> getEventsForBeneficiary(UUID beneficiaryId) {
        if (!beneficiaryRepository.existsById(beneficiaryId)) {
            throw new NoSuchElementException("Beneficiary not found: " + beneficiaryId);
        }
        List<EventBeneficiaryEntity> associations = eventBeneficiaryRepository.findByIdBeneficiaryId(beneficiaryId);
        return associations.stream()
                .map(a -> eventMapper.toDto(a.getEvent()))
                .toList();
    }

    /**
     * Searches beneficiaries by name, organization, or city.
     */
    @Transactional(readOnly = true)
    public Page<BeneficiaryDto> searchBeneficiaries(String query, Pageable pageable) {
        return beneficiaryRepository.search(query, pageable)
                .map(beneficiaryMapper::toDto);
    }

    private BeneficiaryEntity findOrThrow(UUID id) {
        // findById() alone does not enforce tenant isolation on this codebase's Hibernate version —
        // see docs/specs/platform-hardening/ Finding 0 / Requirement 0.
        Optional<BeneficiaryEntity> beneficiary = TenantContext.isPresent()
                ? beneficiaryRepository.findByIdAndTenantId(id, TenantContext.getCurrentTenantId())
                : beneficiaryRepository.findById(id);
        return beneficiary.orElseThrow(() -> new NoSuchElementException("Beneficiary not found: " + id));
    }
}
