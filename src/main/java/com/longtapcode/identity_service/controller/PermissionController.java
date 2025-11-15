package com.longtapcode.identity_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.PermissionRequest;
import com.longtapcode.identity_service.dto.response.PermissionResponse;
import com.longtapcode.identity_service.service.PermissionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionController {
    PermissionService permissionService;

    @PostMapping("/createPermission")
    public ApiResponse<String> createPermission(@RequestBody PermissionRequest request) {
        permissionService.createPermission(request);
        return ApiResponse.<String>builder()
                .result("create permission successful")
                .build();
    }

    @GetMapping("getAllPermission")
    public ApiResponse<List<PermissionResponse>> getAllPermission() {
        var permissions = permissionService.getAllPermission();
        return ApiResponse.<List<PermissionResponse>>builder()
                .result(permissions)
                .build();
    }

    @DeleteMapping("deletePermission/{permission}")
    public ApiResponse<String> deleteRole(@PathVariable("permission") String permission) {
        permissionService.deletePermission(permission);
        return ApiResponse.<String>builder()
                .result("delete permission successful")
                .build();
    }
}
