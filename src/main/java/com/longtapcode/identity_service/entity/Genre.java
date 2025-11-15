package com.longtapcode.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "genres")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Genre {
    @Id
    @Column(name = "GenreID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @Size(max = 100)
    @NotNull
    @Column(name = "Name", nullable = false, length = 100)
     String name;

}