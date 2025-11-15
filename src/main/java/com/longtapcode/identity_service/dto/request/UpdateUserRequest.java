package com.longtapcode.identity_service.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRequest {
    @Size(min = 3, message = "INVALID_USERNAME")
    String userName;
    String firstName;
    String lastName;
    String phoneNumber;
    String address;
    String emailAddress;
    String avatar;
    LocalDate dob;
}
