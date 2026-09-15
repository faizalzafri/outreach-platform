package com.outreach.platform.event.mapper;

import com.outreach.platform.event.entity.VolunteerEntity;
import com.outreach.platform.event.model.dto.VolunteerDto;
import com.outreach.platform.event.model.dto.VolunteerImportRequest;
import com.outreach.platform.event.model.dto.VolunteerProfileUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availability", ignore = true)
    @Mapping(target = "totalEventsParticipated", ignore = true)
    @Mapping(target = "avgFeedbackScore", ignore = true)
    @Mapping(target = "lastParticipatedAt", ignore = true)
    VolunteerEntity toEntity(VolunteerImportRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "availability", ignore = true)
    @Mapping(target = "totalEventsParticipated", ignore = true)
    @Mapping(target = "avgFeedbackScore", ignore = true)
    @Mapping(target = "lastParticipatedAt", ignore = true)
    void updateEntityFromImportRequest(VolunteerImportRequest request, @MappingTarget VolunteerEntity entity);
}
