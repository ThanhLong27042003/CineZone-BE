package com.longtapcode.identity_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.longtapcode.identity_service.dto.request.RoleRequest;
import com.longtapcode.identity_service.dto.response.RoleResponse;
import com.longtapcode.identity_service.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    List<RoleResponse> toListRoleResponse(List<Role> roles);
}
