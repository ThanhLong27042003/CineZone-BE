package com.longtapcode.identity_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.longtapcode.identity_service.dto.request.CreationUserRequest;
import com.longtapcode.identity_service.dto.request.UpdateUserRequest;
import com.longtapcode.identity_service.dto.response.UserResponse;
import com.longtapcode.identity_service.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(CreationUserRequest request);

    UserResponse toUserResponse(User user);

    List<UserResponse> toListUserResponse(List<User> users);

    void updateUser(@MappingTarget User user, UpdateUserRequest request);
}
