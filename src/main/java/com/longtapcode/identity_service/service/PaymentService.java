//package com.longtapcode.identity_service.service;
//
//import com.longtapcode.identity_service.dto.event.BookingConfirmedEvent;
//import com.longtapcode.identity_service.dto.event.SeatInfoEvent;
//import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
//import com.longtapcode.identity_service.dto.response.PaymentCallbackResponse;
//import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
//import com.longtapcode.identity_service.entity.*;
//import com.longtapcode.identity_service.constant.SeatInstanceStatus;
//import com.longtapcode.identity_service.exception.AppException;
//import com.longtapcode.identity_service.exception.ErrorCode;
//import com.longtapcode.identity_service.repository.*;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.*;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class PaymentService {
//
//    private final UserRepository userRepository;
//    private final ShowRepository showRepository;
//    private final BookingRepository bookingRepository;
//    private final BookingDetailRepository bookingDetailRepository;
//    private final RedisTemplate<String, String> redisTemplate;
//    private final SimpMessagingTemplate messagingTemplate;
//
//    private final VNPayService vnPayService;
//    private final PayPalService payPalService;
//
//    private final KafkaProducerService kafkaProducerService;
//    @PreAuthorize("#request.userId == authentication.principal.claims['userId']")
//    public PaymentCreateResponse createPayment(PaymentCreateRequest request, HttpServletRequest httpRequest) {
//        log.info("Creating payment - Method: {}, ShowId: {}, User: {}",
//                request.getPaymentMethod(), request.getShowId(), request.getUserId());
//
//        validateSeatsHeld(request);
//
//        switch (request.getPaymentMethod().toLowerCase()) {
//            case "vnpay":
//                return vnPayService.createVNPayPayment(request, httpRequest);
//
//            case "paypal":
//                return payPalService.createPayPalOrder(request);
//
//            default:
//                throw new AppException(ErrorCode.PAYMENT_FAILED);
//        }
//    }
//
//
//    private void validateSeatsHeld(PaymentCreateRequest request) {
//        for (String seatNumber : request.getSeatNumbers()) {
//            String holdKey = "hold:" + request.getShowId() + ":" + seatNumber;
//            String heldByUser = redisTemplate.opsForValue().get(holdKey);
//
//            if (heldByUser == null || !heldByUser.equals(request.getUserId())) {
//                log.warn("Seat {} is not held by user {}", seatNumber, request.getUserId());
//                throw new AppException(ErrorCode.SEAT_NOT_AVAILABLE);
//            }
//        }
//    }
//
//    @Transactional
//    public PaymentCallbackResponse processVNPayCallback(Map<String, String> params) {
//        log.info("Processing VNPay callback");
//
//        Map<String, Object> result = vnPayService.verifyVNPayPayment(params);
//        return completeBooking(result);
//    }
//
//
//    @Transactional
//    public PaymentCallbackResponse processPayPalCallback(String paypalOrderId, String token) {
//        log.info("Processing PayPal callback - OrderId: {}, Token: {}", paypalOrderId, token);
//
//        Map<String, Object> result = payPalService.capturePayPalOrder(paypalOrderId);
//        return completeBooking(result);
//    }
//
//    private PaymentCallbackResponse completeBooking(Map<String, Object> paymentResult) {
//        Boolean success = (Boolean) paymentResult.get("success");
//
//        if (!success) {
//            String userId = (String) paymentResult.get("userId");
//            Long showId = (Long) paymentResult.get("showId");
//            String[] seats = (String[]) paymentResult.get("seats");
//
//            unlockSeats(showId, seats, userId);
//            throw new AppException(ErrorCode.PAYMENT_FAILED);
//        }
//
//        String orderId = (String) paymentResult.get("orderId");
//        Long showId = (Long) paymentResult.get("showId");
//        String userId = (String) paymentResult.get("userId");
//        String[] seatNumbers = (String[]) paymentResult.get("seats");
//        Long amount = (Long) paymentResult.get("amount");
//        String paymentMethod = (String) paymentResult.get("paymentMethod");
//
//        // Get entities
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
//
//        Show show = showRepository.findById(showId)
//                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));
//
//        // Create Booking
//        Booking booking = Booking.builder()
//                .id1(user)
//                .showID(show)
//                .bookingDate(LocalDateTime.now())
//                .totalPrice(amount)
//                .paymentMethod(paymentMethod)
//                .orderId(orderId)
//                .status("CONFIRMED")
//                .build();
//
//        bookingRepository.save(booking);
//        log.info("Created booking with ID: {}", booking.getId());
//
//        Set<BookingDetail> bookingDetails = new HashSet<>();
//
//        for (String seatNumber : seatNumbers) {
//            String holdKey = "hold:" + showId + ":" + seatNumber;
//            String bookedKey = "booked:" + showId + ":" + seatNumber;
//
//            redisTemplate.opsForValue().set(bookedKey, userId);
//            redisTemplate.delete(holdKey);
//
//            BookingDetail bookingDetail = BookingDetail.builder()
//                    .bookingID(booking)
//                    .seatNumber(seatNumber)
//                    .price(show.getPrice())
//                    .build();
//
//            bookingDetails.add(bookingDetail);
//        }
//
//        bookingDetailRepository.saveAll(bookingDetails);
//        log.info("Created {} booking details", bookingDetails.size());
//
//        Set<String> seatNumberSet = Set.of(seatNumbers);
//        PaymentCreateResponse wsMessage = PaymentCreateResponse.builder()
//                .showId(showId)
//                .userId(userId)
//                .seatNumbers(seatNumberSet)
//                .status(SeatInstanceStatus.BOOKED.getStatus())
//                .expiresAt(0L)
//                .build();
//
//        messagingTemplate.convertAndSend("/topic/show/" + showId, wsMessage);
//        log.info("Broadcasted BOOKED status for show: {}", showId);
//
//        publishBookingConfirmedEvent(booking, show, user, seatNumbers, paymentResult);
//
//        return PaymentCallbackResponse.builder()
//                .success(true)
//                .bookingId(booking.getId())
//                .orderId(orderId)
//                .message("Đặt vé thành công!")
//                .build();
//    }
//
//    private void publishBookingConfirmedEvent(Booking booking, Show show, User user,
//                                              String[] seatNumbers, Map<String, Object> paymentResult){
//        try{
//
//            Set<SeatInfoEvent> seats = new HashSet<>();
//            for (String seatNumber : seatNumbers) {
//                seats.add(SeatInfoEvent.builder()
//                        .seatNumber(seatNumber)
//                        .build());
//            }
//
//            // Build event
//            BookingConfirmedEvent event = BookingConfirmedEvent.builder()
//                    // Booking info
//                    .bookingId(booking.getId())
//                    .orderId(booking.getOrderId())
//                    .bookingDate(booking.getBookingDate())
//                    .totalPrice(booking.getTotalPrice())
//                    .paymentMethod(booking.getPaymentMethod())
//                    .transactionId((String) paymentResult.getOrDefault("transactionId", "N/A"))
//
//                    // User info
//                    .userId(user.getId())
//                    .userEmail(user.getEmailAddress())
//                    .userName(user.getUserName())
//
//                    // Show info
//                    .showId(show.getId())
//                    .movieTitle(show.getMovieID().getTitle())
//                    .showDateTime(show.getShowDateTime())
//
//                    .seats(seats)
//
//                    .eventTime(LocalDateTime.now())
//                    .build();
//
//            // Publish to Kafka
//            kafkaProducerService.publishBookingConfirmed(event);
//
//            log.info("Published BookingConfirmedEvent for booking: {}", booking.getId());
//        }catch(Exception e){
//            log.error("Failed to publish BookingConfirmedEvent for booking: {}", booking.getId(), e);
//        }
//
//    }
//
//    private void unlockSeats(Long showId, String[] seatNumbers, String userId) {
//        log.info("Unlocking seats for failed payment - ShowId: {}, User: {}", showId, userId);
//
//        for (String seatNumber : seatNumbers) {
//            String holdKey = "hold:" + showId + ":" + seatNumber;
//            String heldByUser = redisTemplate.opsForValue().get(holdKey);
//
//            if (userId.equals(heldByUser)) {
//                redisTemplate.delete(holdKey);
//            }
//        }
//
//        Set<String> seatNumberSet = Set.of(seatNumbers);
//        PaymentCreateResponse wsMessage = PaymentCreateResponse.builder()
//                .showId(showId)
//                .userId(userId)
//                .seatNumbers(seatNumberSet)
//                .status(SeatInstanceStatus.AVAILABLE.getStatus())
//                .expiresAt(0L)
//                .build();
//
//        messagingTemplate.convertAndSend("/topic/show/" + showId, wsMessage);
//    }
//}

package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.event.BookingConfirmedEvent;
import com.longtapcode.identity_service.dto.event.SeatInfoEvent;
import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCallbackResponse;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.entity.*;
import com.longtapcode.identity_service.constant.SeatInstanceStatus;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final VNPayService vnPayService;
    private final PayPalService payPalService;
    private final KafkaProducerService kafkaProducerService;

    // ==================== TẠO PAYMENT ====================

    @PreAuthorize("#request.userId == authentication.principal.claims['userId']")
    @Transactional
    public PaymentCreateResponse createPayment(PaymentCreateRequest request, HttpServletRequest httpRequest) {
        log.info("Creating payment - Method: {}, ShowId: {}, User: {}",
                request.getPaymentMethod(), request.getShowId(), request.getUserId());

        validateSeatsHeld(request);

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        if (show.getShowDateTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.SHOW_ALREADY_PASSED);
        }

        String orderId = UUID.randomUUID().toString();
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Booking pendingBooking = Booking.builder()
                .id1(user)
                .showID(show)
                .orderId(orderId)
                .totalPrice(request.getAmount())
                .paymentMethod(request.getPaymentMethod().toUpperCase())
                .status("PAYMENT_PROCESSING")
                .bookingDate(LocalDateTime.now())
                .build();

        bookingRepository.save(pendingBooking);
        log.info("Created pending booking with ID: {} - Status: PAYMENT_PROCESSING", pendingBooking.getId());

        // Tạo payment URL
        PaymentCreateResponse response;
        switch (request.getPaymentMethod().toLowerCase()) {
            case "vnpay":
                response = vnPayService.createVNPayPayment(request, httpRequest);
                break;
            case "paypal":
                response = payPalService.createPayPalOrder(request, orderId);
                break;
            default:
                pendingBooking.setStatus("CANCELLED");
                bookingRepository.save(pendingBooking);
                throw new AppException(ErrorCode.PAYMENT_FAILED);
        }

        // Lưu bookingId vào metadata
        String metadataKey = request.getPaymentMethod().toLowerCase() + "_metadata:" +
                (request.getPaymentMethod().equalsIgnoreCase("vnpay") ? orderId : response.getOrderId());
        redisTemplate.opsForHash().put(metadataKey, "bookingId", String.valueOf(pendingBooking.getId()));

        return response;
    }

    private void validateSeatsHeld(PaymentCreateRequest request) {
        for (String seatNumber : request.getSeatNumbers()) {
            String holdKey = "hold:" + request.getShowId() + ":" + seatNumber;
            String heldByUser = redisTemplate.opsForValue().get(holdKey);

            if (heldByUser == null || !heldByUser.equals(request.getUserId())) {
                log.warn("Seat {} is not held by user {}", seatNumber, request.getUserId());
                throw new AppException(ErrorCode.SEAT_NOT_AVAILABLE);
            }
        }
    }

    // ==================== XỬ LÝ CALLBACK ====================

    @Transactional
    public PaymentCallbackResponse processVNPayCallback(Map<String, String> params) {
        log.info("Processing VNPay callback");
        Map<String, Object> result = vnPayService.verifyVNPayPayment(params);
        return completeBooking(result);
    }

    @Transactional
    public PaymentCallbackResponse processPayPalCallback(String paypalOrderId, String token) {
        log.info("Processing PayPal callback - OrderId: {}, Token: {}", paypalOrderId, token);
        Map<String, Object> result = payPalService.capturePayPalOrder(paypalOrderId);
        return completeBooking(result);
    }

    private PaymentCallbackResponse completeBooking(Map<String, Object> paymentResult) {
        Boolean success = (Boolean) paymentResult.get("success");
        String orderId = (String) paymentResult.get("orderId");

        Booking booking = bookingRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if (!success) {
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);

            String userId = (String) paymentResult.get("userId");
            Long showId = (Long) paymentResult.get("showId");
            String[] seats = (String[]) paymentResult.get("seats");
            unlockSeats(showId, seats, userId);

            throw new AppException(ErrorCode.PAYMENT_FAILED);
        }

        Long showId = (Long) paymentResult.get("showId");
        String userId = (String) paymentResult.get("userId");
        String[] seatNumbers = (String[]) paymentResult.get("seats");
        Long amount = (Long) paymentResult.get("amount");

        User user = booking.getId1();
        Show show = booking.getShowID();

        booking.setStatus("CONFIRMED");
        booking.setTotalPrice(amount);
        bookingRepository.save(booking);
        log.info("Updated booking {} to CONFIRMED", booking.getId());

        Set<BookingDetail> bookingDetails = new HashSet<>();
        for (String seatNumber : seatNumbers) {
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

        // Broadcast BOOKED status
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

        // Publish Kafka event
        publishBookingConfirmedEvent(booking, show, user, seatNumbers, paymentResult);

        return PaymentCallbackResponse.builder()
                .success(true)
                .bookingId(booking.getId())
                .orderId(orderId)
                .message("Đặt vé thành công!")
                .build();
    }

    private void publishBookingConfirmedEvent(Booking booking, Show show, User user,
                                              String[] seatNumbers, Map<String, Object> paymentResult) {
        try {
            Set<SeatInfoEvent> seats = new HashSet<>();
            for (String seatNumber : seatNumbers) {
                seats.add(SeatInfoEvent.builder()
                        .seatNumber(seatNumber)
                        .build());
            }

            BookingConfirmedEvent event = BookingConfirmedEvent.builder()
                    .bookingId(booking.getId())
                    .orderId(booking.getOrderId())
                    .bookingDate(booking.getBookingDate())
                    .totalPrice(booking.getTotalPrice())
                    .paymentMethod(booking.getPaymentMethod())
                    .transactionId((String) paymentResult.getOrDefault("transactionId", "N/A"))
                    .userId(user.getId())
                    .userEmail(user.getEmailAddress())
                    .userName(user.getUserName())
                    .showId(show.getId())
                    .movieTitle(show.getMovieID().getTitle())
                    .showDateTime(show.getShowDateTime())
                    .seats(seats)
                    .roomName(show.getRoomId().getName())
                    .eventTime(LocalDateTime.now())
                    .build();

            kafkaProducerService.publishBookingConfirmed(event);
            log.info("Published BookingConfirmedEvent for booking: {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish BookingConfirmedEvent for booking: {}", booking.getId(), e);
        }
    }

    private void unlockSeats(Long showId, String[] seatNumbers, String userId) {
        log.info("Unlocking seats for failed payment - ShowId: {}, User: {}", showId, userId);

        for (String seatNumber : seatNumbers) {
            String holdKey = "hold:" + showId + ":" + seatNumber;
            String heldByUser = redisTemplate.opsForValue().get(holdKey);

            if (userId.equals(heldByUser)) {
                redisTemplate.delete(holdKey);
            }
        }

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

    // ==================== TỰ ĐỘNG HỦY BOOKING HẾT HẠN ====================

    @Scheduled(fixedDelay = 60000) // Chạy mỗi 1 phút
    @Transactional
    public void cancelExpiredPaymentProcessing() {
        LocalDateTime thirtyMinutesAgo = LocalDateTime.now().minusMinutes(30);

        List<Booking> expiredBookings = bookingRepository.findByStatusAndBookingDateBefore(
                "PAYMENT_PROCESSING",
                thirtyMinutesAgo
        );

        for (Booking booking : expiredBookings) {
            log.info("Cancelling expired booking: {}", booking.getId());
            booking.setStatus("EXPIRED");
            bookingRepository.save(booking);
        }
    }
}