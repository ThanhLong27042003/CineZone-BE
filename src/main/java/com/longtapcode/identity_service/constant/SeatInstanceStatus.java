package com.longtapcode.identity_service.constant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum SeatInstanceStatus {
    HELD("HELD"),
    AVAILABLE("AVAILABLE"),
    BOOKED("BOOKED");
    String status;
}
