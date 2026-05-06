package com.longtapcode.identity_service.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

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
    String showStatus;
}
