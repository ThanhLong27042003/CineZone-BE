package com.longtapcode.identity_service.service;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.longtapcode.identity_service.dto.request.PermissionRequest;
import com.longtapcode.identity_service.dto.response.PermissionResponse;
import com.longtapcode.identity_service.mapper.PermissionMapper;
import com.longtapcode.identity_service.repository.PermissionRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionService {
    PermissionRepository permissionRepository;
    PermissionMapper permissionMapper;

    @PreAuthorize("hasRole('ADMIN')")
    public void createPermission(PermissionRequest request) {
        var permission = permissionMapper.toPermission(request);
        permissionRepository.save(permission);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<PermissionResponse> getAllPermission() {
        var permissions = permissionRepository.findAll();
        return permissionMapper.toListPermissionResponse(permissions);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deletePermission(String permission) {
        permissionRepository.deleteById(permission);
    }
}
