package com.longtapcode.identity_service.configuration;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.longtapcode.identity_service.constant.PredefinedRole;
import com.longtapcode.identity_service.entity.Role;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.repository.RoleRepository;
import com.longtapcode.identity_service.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ApplicationInitConfig {
    UserRepository userRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${jwt.admin_password}")
    String adminPassword;

    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            Field[] allRole = PredefinedRole.class.getDeclaredFields();
            Arrays.stream(allRole).forEach(fieldRole -> {
                Role role = new Role();
                try {
                    role.setName(fieldRole.get(null).toString());
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                roleRepository.save(role);
            });
            if (userRepository.findByUserName("admin").isEmpty()) {
                Set<Role> adminRoles = new HashSet<>();
                Role adminRole = new Role();
                adminRole.setName(PredefinedRole.ADMIN_ROLE);
                adminRoles.add(adminRole);
                User user = User.builder()
                        .userName("admin")
                        .password(passwordEncoder.encode(adminPassword))
                        .roles(adminRoles)
                        .dob(LocalDate.now())
                        .build();
                userRepository.save(user);
            }
        };
    }
}
