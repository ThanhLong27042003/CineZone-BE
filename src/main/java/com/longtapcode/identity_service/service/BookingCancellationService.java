package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.constant.SeatInstanceStatus;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.BookingDetail;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.BookingDetailRepository;
import com.longtapcode.identity_service.repository.BookingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingCancellationService {

    BookingRepository bookingRepository;
    BookingDetailRepository bookingDetailRepository;
    PayPalService payPalService;
    VNPayService vnPayService;
    StringRedisTemplate redisTemplate;
    SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Map<String, Object> cancelBookingWithRefund(Long bookingId) {
        // 1. Validate booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        // Check status
        if (!"CONFIRMED".equals(booking.getStatus())) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }

        // Check time limit (2 hours before show)
        LocalDateTime showTime = booking.getShowID().getShowDateTime();
        LocalDateTime now = LocalDateTime.now();
        long hoursUntilShow = java.time.Duration.between(now, showTime).toHours();

        if (hoursUntilShow < 2) {
            throw new RuntimeException("Cannot cancel booking less than 2 hours before show time");
        }

        // 2. Process refund based on payment method
        Map<String, Object> refundResult = null;
        String paymentMethod = booking.getPaymentMethod();

        try {
            if ("PAYPAL".equalsIgnoreCase(paymentMethod)) {
                refundResult = refundPayPal(booking);
            } else if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                refundResult = refundVNPay(booking);
            } else {
                throw new RuntimeException("Unsupported payment method: " + paymentMethod);
            }

            // 3. Update booking status
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);

            // 4. Release seats
            releaseBookedSeats(booking);

            log.info("Booking {} cancelled successfully with refund", bookingId);

            return Map.of(
                    "success", true,
                    "message", "Booking cancelled and refund processed",
                    "refundId", refundResult.get("refundId"),
                    "refundAmount", refundResult.get("amount")
            );

        } catch (Exception e) {
            log.error("Failed to cancel booking with refund: {}", bookingId, e);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> refundPayPal(Booking booking) {
        String transactionId = booking.getTransactionId();
        BigDecimal amount = booking.getTotalPrice();

        if (transactionId == null || transactionId.isEmpty()) {
            throw new RuntimeException("Transaction ID not found for PayPal refund");
        }

        log.info("Processing PayPal refund for booking: {}", booking.getId());
        return payPalService.refundPayPalPayment(transactionId, amount);
    }

    private Map<String, Object> refundVNPay(Booking booking) {
        // VNPay refund logic (if supported)
        // Note: VNPay might not support automatic refunds via API
        // You may need manual refund process

        log.warn("VNPay refund not implemented - Manual refund required for booking: {}",
                booking.getId());

        return Map.of(
                "refundId", "MANUAL_VNPAY",
                "amount", booking.getTotalPrice(),
                "message", "VNPay refund requires manual processing"
        );
    }

    private void releaseBookedSeats(Booking booking) {
        Long showId = booking.getShowID().getId();

        // Get all booking details
        List<BookingDetail> details = bookingDetailRepository.findAll()
                .stream()
                .filter(d -> d.getBookingID().getId().equals(booking.getId()))
                .toList();

        Set<String> seatNumbers = new HashSet<>();

        for (BookingDetail detail : details) {
            String seatNumber = detail.getSeatNumber();
            seatNumbers.add(seatNumber);

            // Remove from Redis
            String bookedKey = "booked:" + showId + ":" + seatNumber;
            redisTemplate.delete(bookedKey);

            log.info("Released seat {} for show {}", seatNumber, showId);
        }

        // Broadcast seat release via WebSocket
        PaymentCreateResponse wsMessage = PaymentCreateResponse.builder()
                .showId(showId)
                .userId(booking.getId1().getId())
                .seatNumbers(seatNumbers)
                .status(SeatInstanceStatus.AVAILABLE.getStatus())
                .expiresAt(0L)
                .build();

        messagingTemplate.convertAndSend("/topic/show/" + showId, wsMessage);
        log.info("Broadcasted seat release for show: {}", showId);
    }
}