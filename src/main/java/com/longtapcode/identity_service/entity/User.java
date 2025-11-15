package com.longtapcode.identity_service.entity;

import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @ManyToMany
    Set<Role> roles;
}
