package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCallbackResponse;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.BookingDetail;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.constant.SeatInstanceStatus;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final UserRepository userRepository;
    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    // Payment Gateway Services
    private final VNPayService vnPayService;  // Tách VNPay logic sang VNPayService
    private final PayPalService payPalService;

    /**
     * ✅ Route payment request đến gateway tương ứng
     */
    public PaymentCreateResponse createPayment(PaymentCreateRequest request, HttpServletRequest httpRequest) {
        log.info("Creating payment - Method: {}, ShowId: {}, User: {}",
                request.getPaymentMethod(), request.getShowId(), request.getUserId());

        // 1. Validate common logic
        validateSeatsHeld(request);

        // 2. Route to appropriate gateway
        switch (request.getPaymentMethod().toLowerCase()) {
            case "vnpay":
                return vnPayService.createVNPayPayment(request, httpRequest);

            case "paypal":
                return payPalService.createPayPalOrder(request);

            default:
                throw new AppException(ErrorCode.PAYMENT_FAILED);
        }
    }

    /**
     * ✅ Validate seats vẫn được hold bởi user
     */
    private void validateSeatsHeld(PaymentCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        for (String seatNumber : request.getSeatNumbers()) {
            String holdKey = "hold:" + request.getShowId() + ":" + seatNumber;
            String heldByUser = redisTemplate.opsForValue().get(holdKey);

            if (heldByUser == null || !heldByUser.equals(request.getUserId())) {
                log.warn("Seat {} is not held by user {}", seatNumber, request.getUserId());
                throw new AppException(ErrorCode.SEAT_NOT_AVAILABLE);
            }
        }
    }

    /**
     * ✅ VNPay callback handler
     */
    @Transactional
    public PaymentCallbackResponse processVNPayCallback(Map<String, String> params) {
        log.info("Processing VNPay callback");

        Map<String, Object> result = vnPayService.verifyVNPayPayment(params);
        return completeBooking(result);
    }

    /**
     * ✅ PayPal callback handler
     */
    @Transactional
    public PaymentCallbackResponse processPayPalCallback(String paypalOrderId, String token) {
        log.info("Processing PayPal callback - OrderId: {}, Token: {}", paypalOrderId, token);

        Map<String, Object> result = payPalService.capturePayPalOrder(paypalOrderId);
        return completeBooking(result);
    }

    /**
     * ✅ Common booking creation logic
     */
    private PaymentCallbackResponse completeBooking(Map<String, Object> paymentResult) {
        Boolean success = (Boolean) paymentResult.get("success");

        if (!success) {
            String userId = (String) paymentResult.get("userId");
            Long showId = (Long) paymentResult.get("showId");
            String[] seats = (String[]) paymentResult.get("seats");

            unlockSeats(showId, seats, userId);
            throw new AppException(ErrorCode.PAYMENT_FAILED);
        }

        // Extract payment info
        String orderId = (String) paymentResult.get("orderId");
        Long showId = (Long) paymentResult.get("showId");
        String userId = (String) paymentResult.get("userId");
        String[] seatNumbers = (String[]) paymentResult.get("seats");
        Long amount = (Long) paymentResult.get("amount");
        String paymentMethod = (String) paymentResult.get("paymentMethod");

        // Get entities
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        // Create Booking
        Booking booking = Booking.builder()
                .id1(user)
                .showID(show)
                .bookingDate(LocalDateTime.now())
                .totalPrice(amount) // ✅ Amount in cents, convert if needed
                .paymentMethod(paymentMethod)
                .orderId(orderId)
                .status("CONFIRMED")
                .build();

        bookingRepository.save(booking);
        log.info("Created booking with ID: {}", booking.getId());

        // Create BookingDetails and update Redis
        Set<BookingDetail> bookingDetails = new HashSet<>();

        for (String seatNumber : seatNumbers) {
            // Update Redis: HELD → BOOKED
            String holdKey = "hold:" + showId + ":" + seatNumber;
            String bookedKey = "booked:" + showId + ":" + seatNumber;

            redisTemplate.opsForValue().set(bookedKey, userId);
            redisTemplate.delete(holdKey);

            BookingDetail bookingDetail = BookingDetail.builder()
                    .bookingID(booking)
                    .seatNumber(seatNumber)
                    .price(show.getPrice())
                    .build();

            bookingDetails.add(bookingDetail);
        }

        bookingDetailRepository.saveAll(bookingDetails);
        log.info("Created {} booking details", bookingDetails.size());

        // Broadcast WebSocket
        Set<String> seatNumberSet = Set.of(seatNumbers);
        PaymentCreateResponse wsMessage = PaymentCreateResponse.builder()
                .showId(showId)
                .userId(userId)
                .seatNumbers(seatNumberSet)
                .status(SeatInstanceStatus.BOOKED.getStatus())
                .expiresAt(0L)
                .build();

        messagingTemplate.convertAndSend("/topic/show/" + showId, wsMessage);
        log.info("Broadcasted BOOKED status for show: {}", showId);

        return PaymentCallbackResponse.builder()
                .success(true)
                .bookingId(booking.getId())
                .orderId(orderId)
                .message("Đặt vé thành công!")
                .build();
    }

    /**
     * ✅ Unlock seats on payment failure
     */
    private void unlockSeats(Long showId, String[] seatNumbers, String userId) {
        log.info("Unlocking seats for failed payment - ShowId: {}, User: {}", showId, userId);

        for (String seatNumber : seatNumbers) {
            String holdKey = "hold:" + showId + ":" + seatNumber;
            String heldByUser = redisTemplate.opsForValue().get(holdKey);

            if (userId.equals(heldByUser)) {
                redisTemplate.delete(holdKey);
            }
        }

        // Broadcast unlock
        Set<String> seatNumberSet = Set.of(seatNumbers);
        PaymentCreateResponse wsMessage = PaymentCreateResponse.builder()
                .showId(showId)
                .userId(userId)
                .seatNumbers(seatNumberSet)
                .status(SeatInstanceStatus.AVAILABLE.getStatus())
                .expiresAt(0L)
                .build();

        messagingTemplate.convertAndSend("/topic/show/" + showId, wsMessage);
    }
}