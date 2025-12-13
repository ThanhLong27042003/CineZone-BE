package com.longtapcode.identity_service.dto.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowResponse {
    Long showId;
    Long movieId;
    Long roomId;
    String roomName;
    LocalDate showDate;
    LocalTime showTime;
    BigDecimal price;
}
