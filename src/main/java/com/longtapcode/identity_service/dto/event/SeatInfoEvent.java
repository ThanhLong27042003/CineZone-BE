package com.longtapcode.identity_service.dto.event;

import java.io.Serializable;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SeatInfoEvent implements Serializable {
    String seatNumber;
}
