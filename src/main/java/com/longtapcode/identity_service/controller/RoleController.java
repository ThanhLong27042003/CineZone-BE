package com.longtapcode.identity_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.RoleRequest;
import com.longtapcode.identity_service.dto.response.RoleResponse;
import com.longtapcode.identity_service.service.RoleService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleController {
    RoleService roleService;

    @PutMapping("/updateRole")
    public ApiResponse<String> updateRole(@RequestBody RoleRequest request) {
        roleService.updateRole(request);
        return ApiResponse.<String>builder().result("update role successful").build();
    }

    @GetMapping("/getAllRole")
    public ApiResponse<List<RoleResponse>> getAllRole() {
        var roles = roleService.getAllRole();
        return ApiResponse.<List<RoleResponse>>builder().result(roles).build();
    }

    @DeleteMapping("/deleteRole/{role}")
    public ApiResponse<String> deleteRole(@PathVariable("role") String role) {
        roleService.deleteRole(role);
        return ApiResponse.<String>builder().result("delete role successful").build();
    }
}
