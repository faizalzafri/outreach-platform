package com.outreach.platform.event.mapper;

import com.outreach.platform.event.entity.BeneficiaryEntity;
import com.outreach.platform.event.model.dto.BeneficiaryDto;
import org.mapstruct.Mapper;

/**
 * Maps between BeneficiaryEntity and beneficiary DTOs.
 */
@Mapper(componentModel = "spring")
public interface BeneficiaryMapper {

    BeneficiaryDto toDto(BeneficiaryEntity entity);
}
