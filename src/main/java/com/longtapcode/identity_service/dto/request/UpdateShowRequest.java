package com.longtapcode.identity_service.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateShowRequest {
    Long movieId;
    Long roomId;
    LocalDate showDate;
    LocalTime showTime;
    BigDecimal price;
}
