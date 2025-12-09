package com.longtapcode.identity_service.dto.response;

import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

public class BookingResponse {
    Long id;
    String userId;
    Long showId;
    String orderId;
    String paymentMethod;
    Long totalPrice;
    String status;
    LocalDateTime bookingDate;
}
