package com.longtapcode.identity_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatUpdateResponse {
    Long showId;
    String userId;
    String seatNumber;
    String status;
    Long expiresAt;
}
