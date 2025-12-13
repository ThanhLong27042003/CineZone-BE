package com.longtapcode.identity_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {
    Long id;
    String orderId;
    String userId;
    String userName;
    String userEmail;
    Long showId;
    String movieTitle;
    LocalDateTime showDateTime;
    String paymentMethod;
    Long totalPrice;
    String status;
    LocalDateTime bookingDate;
    Set<BookingDetailResponse> bookingDetails;
}