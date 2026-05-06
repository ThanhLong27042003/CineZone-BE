package com.longtapcode.identity_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "shows")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class Show {
    @Id
    @Column(name = "ShowID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "MovieID")
    Movie movieID;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "RoomId")
    Room roomId;

    @NotNull
    @Column(name = "ShowDateTime", nullable = false)
    LocalDateTime showDateTime;

    @NotNull
    @Column(name = "Price", nullable = false, precision = 10, scale = 2)
    BigDecimal price;
}
