package com.longtapcode.identity_service.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentCreateRequest {
     Long showId;
     Set<String> seatNumbers;
     String userId;
     BigDecimal amount;
     String paymentMethod;
     String returnUrl;

}
