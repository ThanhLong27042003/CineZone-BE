package com.longtapcode.identity_service.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.longtapcode.identity_service.dto.request.RoleRequest;
import com.longtapcode.identity_service.dto.response.RoleResponse;
import com.longtapcode.identity_service.entity.Permission;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.RoleMapper;
import com.longtapcode.identity_service.repository.PermissionRepository;
import com.longtapcode.identity_service.repository.RoleRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;

    @PreAuthorize("hasRole('ADMIN')")
    public void updateRole(RoleRequest request) {
        var role = roleRepository
                .findById(request.getName())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_EXISTED));
        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(request.getPermissions()));
        role.setPermissions(permissions);
        roleRepository.save(role);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<RoleResponse> getAllRole() {
        var roles = roleRepository.findAll();
        return roleMapper.toListRoleResponse(roles);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteRole(String role) {
        roleRepository.deleteById(role);
    }
}
