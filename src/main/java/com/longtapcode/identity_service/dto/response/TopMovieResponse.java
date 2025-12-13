package com.longtapcode.identity_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TopMovieResponse {
    String title;
    Long bookingCount;
    Long revenue;
}