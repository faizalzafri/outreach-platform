package com.outreach.platform.feedback.mapper;

import com.outreach.platform.feedback.entity.VolunteerFeedbackEntity;
import com.outreach.platform.feedback.model.dto.FeedbackDto;
import com.outreach.platform.feedback.model.dto.FeedbackSubmitRequest;
import com.outreach.platform.feedback.model.dto.FeedbackUpdateRequest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for converting between feedback entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(source = "createdDate", target = "createdAt")
    @Mapping(source = "lastModifiedDate", target = "updatedAt")
    FeedbackDto toDto(VolunteerFeedbackEntity entity);

    List<FeedbackDto> toDtoList(List<VolunteerFeedbackEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sentiment", ignore = true)
    @Mapping(target = "status", constant = "SUBMITTED")
    @Mapping(target = "submittedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    VolunteerFeedbackEntity toEntity(FeedbackSubmitRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "volunteerId", ignore = true)
    @Mapping(target = "anonymous", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "reviewedAt", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    void updateEntityFromRequest(FeedbackUpdateRequest request, @MappingTarget VolunteerFeedbackEntity entity);
}
