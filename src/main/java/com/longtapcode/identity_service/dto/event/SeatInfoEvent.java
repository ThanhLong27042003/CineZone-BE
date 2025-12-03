package com.longtapcode.identity_service.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatInfoEvent implements Serializable {
    String seatNumber;
}
