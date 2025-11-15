package com.longtapcode.identity_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;


@Entity
@Table(name = "bookingdetails")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetail {
    @Id
    @Column(name = "BookingDetailID", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "BookingID")
     Booking bookingID;

    @Size(max = 10)
    @NotNull
    @Column(name = "SeatNumber", nullable = false, length = 10)
     String seatNumber;

    @Column(name = "Price", nullable = false, precision = 10, scale = 2)
     BigDecimal price;

}