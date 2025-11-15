package com.longtapcode.identity_service.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShowRequest {
    Long movieId;
    LocalDateTime showDateTime;
    BigDecimal price;
}
