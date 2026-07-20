package com.outreach.platform.event.mapper;

import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.model.dto.VolunteerDto;
import org.mapstruct.Mapper;

/**
 * Maps between VolunteerEntity and volunteer DTOs.
 */
@Mapper(componentModel = "spring")
public interface VolunteerMapper {

    VolunteerDto toDto(VolunteerEntity entity);
}
