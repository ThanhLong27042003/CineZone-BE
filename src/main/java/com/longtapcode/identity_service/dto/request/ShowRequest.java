package com.longtapcode.identity_service.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowRequest {
    Long movieId;
    Long roomId;
    LocalDate showDate;
    LocalTime showTime;
    BigDecimal price;
}
