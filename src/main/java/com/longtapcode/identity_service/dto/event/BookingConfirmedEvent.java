package com.longtapcode.identity_service.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingConfirmedEvent implements Serializable {
    static final long serialVersionUID = 1L;

    long bookingId;
    String orderId;
    LocalDateTime bookingDate;
    long totalPrice;
    String paymentMethod;
    String transactionId;

    String userId;
    String userEmail;
    String userName;

    long showId;
    String movieTitle;
    LocalDateTime showDateTime;

    Set<SeatInfoEvent> seats;

    LocalDateTime eventTime;

}
