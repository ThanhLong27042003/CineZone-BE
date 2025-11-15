package com.longtapcode.identity_service.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatPaymentResponse {
    Long showId;
    String userId;
    Set<String> seatNumbers;
    String status;
    Long expiresAt;

}
