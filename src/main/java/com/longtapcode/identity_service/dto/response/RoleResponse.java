package com.longtapcode.identity_service.dto.response;

import java.util.Set;

import com.longtapcode.identity_service.entity.Permission;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
    String name;
    Set<Permission> permissions;
}
