package com.longtapcode.identity_service.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreationUserRequest {
    @Size(min = 3, message = "INVALID_USERNAME")
    String userName;

    @Size(min = 8, message = "INVALID_PASSWORD")
    String password;

    String firstName;
    String lastName;
    String phoneNumber;
    String address;
    String emailAddress;
    String avatar;
    LocalDate dob;
}
