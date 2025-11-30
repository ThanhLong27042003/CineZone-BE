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
import com.longtapcode.identity_service.repository.BookingDetailRepository;
import com.longtapcode.identity_service.repository.BookingRepository;
import com.longtapcode.identity_service.repository.ShowRepository;
import com.longtapcode.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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

    @Value("${app.payment.mock-gateway-url:http://localhost:5173/payment/mock}")
    private String mockGatewayUrl;

    @Value("${app.payment.return-url:http://localhost:5173/payment/callback}")
    private String returnUrl;

    /**
     * ✅ Tạo payment URL (KHÔNG động vào logic lock ghế)
     */
    public PaymentCreateResponse createPayment(PaymentCreateRequest request) {
        log.info("Creating payment for showId: {}, user: {}", request.getShowId(), request.getUserId());

        // 1. Validate user và show
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        // 2. Validate seats vẫn được hold bởi user này
        for (String seatNumber : request.getSeatNumbers()) {
            String holdKey = "hold:" + request.getShowId() + ":" + seatNumber;
            String heldByUser = redisTemplate.opsForValue().get(holdKey);

            if (heldByUser == null || !heldByUser.equals(request.getUserId())) {
                log.warn("Seat {} is not held by user {}", seatNumber, request.getUserId());
                throw new AppException(ErrorCode.SEAT_NOT_AVAILABLE);
            }
        }

        // 3. Tính tổng tiền (dùng price từ show)
        int seatCount = request.getSeatNumbers().size();
        BigDecimal price = show.getPrice();

        BigDecimal amount = price.multiply(BigDecimal.valueOf(seatCount));
        Long totalAmount = amount.longValue();

        // 4. Generate orderId
        String orderId = UUID.randomUUID().toString();

        // 5. Build mock payment URL
        String paymentUrl = buildMockPaymentUrl(
                orderId,
                request.getShowId(),
                request.getUserId(),
                request.getSeatNumbers(),
                totalAmount,
                request.getPaymentMethod()
        );

        log.info("Generated payment URL for order: {}", orderId);

        return PaymentCreateResponse.builder()
                .paymentUrl(paymentUrl)
                .orderId(orderId)
                .amount(totalAmount)
                .build();
    }

    /**
     * ✅ Xử lý callback sau khi thanh toán
     */
    @Transactional
    public PaymentCallbackResponse processPaymentCallback(Map<String, String> params) {
        String orderId = params.get("orderId");
        String status = params.get("status");
        Long showId = Long.parseLong(params.get("showId"));
        String userId = params.get("userId");
        String[] seatNumbers = params.get("seats").split(",");
        Long amount = Long.parseLong(params.get("amount"));
        String paymentMethod = params.getOrDefault("paymentMethod", "VNPAY");

        log.info("Processing callback - OrderId: {}, Status: {}, ShowId: {}", orderId, status, showId);

        // 1. Check payment status
        if (!"success".equals(status)) {
            log.warn("Payment failed for order: {}", orderId);
            // Unlock seats nếu payment fail
            unlockSeats(showId, seatNumbers, userId);
            throw new AppException(ErrorCode.PAYMENT_FAILED);
        }

        // 2. Get user và show
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        // 3. Tạo Booking
        Booking booking = Booking.builder()
                .id1(user)
                .showID(show)
                .bookingDate(LocalDateTime.now())
                .totalPrice(amount)
                .paymentMethod(paymentMethod)
                .orderId(orderId)
                .status("CONFIRMED")
                .build();

        bookingRepository.save(booking);
        log.info("Created booking with ID: {}", booking.getId());

        // 4. Tạo BookingDetails và update Redis: HELD → BOOKED
        Set<BookingDetail> bookingDetails = new HashSet<>();

        for (String seatNumber : seatNumbers) {
            // Update Redis
            String holdKey = "hold:" + showId + ":" + seatNumber;
            String bookedKey = "booked:" + showId + ":" + seatNumber;

            redisTemplate.opsForValue().set(bookedKey, userId);
            redisTemplate.delete(holdKey);
            log.debug("Updated Redis: {} -> {}", holdKey, bookedKey);

            // Create booking detail
            BookingDetail bookingDetail = BookingDetail.builder()
                    .bookingID(booking)
                    .seatNumber(seatNumber)
                    .price(show.getPrice())
                    .build();

            bookingDetails.add(bookingDetail);
        }

        bookingDetailRepository.saveAll(bookingDetails);
        log.info("Created {} booking details", bookingDetails.size());

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


    private String buildMockPaymentUrl(String orderId, Long showId, String userId,
                                       Set<String> seatNumbers, Long amount, String paymentMethod) {
        try {
            String seats = String.join(",", seatNumbers);

            return String.format(
                    "%s?orderId=%s&showId=%d&userId=%s&seats=%s&amount=%d&paymentMethod=%s&returnUrl=%s",
                    mockGatewayUrl,
                    orderId,
                    showId,
                    userId,
                    URLEncoder.encode(seats, "UTF-8"),
                    amount,
                    paymentMethod,
                    URLEncoder.encode(returnUrl, "UTF-8")
            );
        } catch (UnsupportedEncodingException e) {
            log.error("Failed to encode payment URL", e);
            throw new RuntimeException("Failed to create payment URL", e);
        }
    }

    /**
     * ✅ Unlock seats nếu payment fail
     */
    private void unlockSeats(Long showId, String[] seatNumbers, String userId) {
        log.info("Unlocking seats for failed payment - ShowId: {}, User: {}", showId, userId);

        for (String seatNumber : seatNumbers) {
            String holdKey = "hold:" + showId + ":" + seatNumber;
            String heldByUser = redisTemplate.opsForValue().get(holdKey);

            if (userId.equals(heldByUser)) {
                redisTemplate.delete(holdKey);
                log.debug("Unlocked seat: {}", seatNumber);
            }
        }

        // Broadcast unlock qua WebSocket
        Set<String> seatNumberSet = Set.of(seatNumbers);
        PaymentCreateResponse wsMessage = PaymentCreateResponse.builder()
                .showId(showId)
                .userId(userId)
                .seatNumbers(seatNumberSet)
                .status(SeatInstanceStatus.AVAILABLE.getStatus())
                .expiresAt(0L)
                .build();

        messagingTemplate.convertAndSend("/topic/show/" + showId, wsMessage);
        log.info("Broadcasted AVAILABLE status after failed payment");
    }
}