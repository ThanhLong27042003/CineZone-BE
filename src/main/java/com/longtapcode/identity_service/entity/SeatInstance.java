package com.longtapcode.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "seat_instances", uniqueConstraints = @UniqueConstraint(columnNames = {"showID","seatID"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seatInstanceID")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "showID", nullable = false)
    Show showId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "seatID", nullable = false)
    Seat seatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    Status status = Status.AVAILABLE;

    @Column(name = "holdExpiresAt")
    LocalDateTime holdExpiresAt;

    public enum Status { AVAILABLE, BOOKED }
}
