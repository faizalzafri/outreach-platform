package com.outreach.platform.event.mapper;

import com.outreach.platform.event.entity.EventEntity;
import com.outreach.platform.event.model.dto.EventCreateRequest;
import com.outreach.platform.event.model.dto.EventDto;
import com.outreach.platform.event.model.dto.EventUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Maps between EventEntity and event DTOs.
 */
@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "updatedAt", source = "lastModifiedDate")
    EventDto toDto(EventEntity entity);

    EventEntity toEntity(EventCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(EventUpdateRequest request, @MappingTarget EventEntity entity);
}
