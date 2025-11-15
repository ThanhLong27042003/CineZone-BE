package com.longtapcode.identity_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.longtapcode.identity_service.entity.Role;

public interface RoleRepository extends JpaRepository<Role, String> {}
