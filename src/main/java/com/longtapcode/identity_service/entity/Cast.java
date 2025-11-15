package com.longtapcode.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "casts")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Cast {
    @Id
    @Column(name = "CastID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "Name", nullable = false)
     String name;

    @Size(max = 255)
    @Column(name = "ProfilePath")
     String profilePath;

}