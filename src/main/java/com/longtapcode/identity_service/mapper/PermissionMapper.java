package com.longtapcode.identity_service.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import com.longtapcode.identity_service.dto.request.PermissionRequest;
import com.longtapcode.identity_service.dto.response.PermissionResponse;
import com.longtapcode.identity_service.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    List<PermissionResponse> toListPermissionResponse(List<Permission> permissions);
}
