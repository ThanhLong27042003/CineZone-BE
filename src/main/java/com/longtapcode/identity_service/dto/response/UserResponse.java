package com.longtapcode.identity_service.dto.response;

import java.time.LocalDate;
import java.util.Set;

import com.longtapcode.identity_service.entity.Role;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {
    String id;
    String userName;
    String password;
    String firstName;
    String lastName;
    String phoneNumber;
    String address;
    String emailAddress;
    String avatar;
    LocalDate dob;
    Set<Role> roles;
}
