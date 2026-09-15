package com.outreach.platform.event.mapper;

import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.model.dto.VolunteerDto;
import com.outreach.platform.event.model.dto.VolunteerProfileUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Maps between VolunteerEntity and volunteer DTOs.
 */
@Mapper(componentModel = "spring")
public interface VolunteerMapper {

    VolunteerDto toDto(VolunteerEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(VolunteerProfileUpdateRequest request, @MappingTarget VolunteerEntity entity);
}
