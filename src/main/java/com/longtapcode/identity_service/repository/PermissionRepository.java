package com.longtapcode.identity_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.longtapcode.identity_service.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, String> {}
