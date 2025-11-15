package com.longtapcode.identity_service.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Entity
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "Seat")
@AllArgsConstructor
@NoArgsConstructor
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SeatID", nullable = false)
    Integer seatId;

    @Column( name = "SeatNumber", nullable = false, length = 10)
    String seatNumber;

    @Column(name = "VIP")
    Integer vip;

}
