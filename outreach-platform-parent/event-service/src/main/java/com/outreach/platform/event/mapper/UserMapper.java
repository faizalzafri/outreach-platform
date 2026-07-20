package com.outreach.platform.event.mapper;

import com.outreach.platform.event.entity.UserEntity;
import com.outreach.platform.event.model.dto.UserDto;
import org.mapstruct.Mapper;

/**
 * Maps between UserEntity and user DTOs.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(UserEntity entity);
}
