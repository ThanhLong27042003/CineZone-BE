package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.configuration.PayPalConfig;
import com.longtapcode.identity_service.constant.SeatInstanceStatus;
import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.ShowRepository;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import com.paypal.payments.RefundRequest;
import com.paypal.payments.CapturesRefundRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {

    private final PayPalHttpClient payPalHttpClient;
    private final PayPalConfig payPalConfig;
    private final ShowRepository showRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;


    public PaymentCreateResponse createPayPalOrder(PaymentCreateRequest request, String orderId) {
        log.info("Creating PayPal order for showId: {}, user: {}", request.getShowId(), request.getUserId());

        // 1. Get show info
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        // 2. Calculate amount
        int seatCount = request.getSeatNumbers().size();
        BigDecimal pricePerSeat = show.getPrice();
        BigDecimal totalAmount = request.getAmount();
        // ✅ PayPal yêu cầu format: "10.00" (2 decimal places)
        String totalAmountStr = totalAmount.setScale(2, RoundingMode.HALF_UP).toString();

        // ✅ Convert to cents/integer for storage (avoid decimal parsing issues)
        Long totalAmountCents = totalAmount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        log.info("PayPal order amount: ${} ({} cents)", totalAmountStr, totalAmountCents);

        // 4. Build PayPal Order Request
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.checkoutPaymentIntent("CAPTURE");

        // Application Context
        ApplicationContext applicationContext = new ApplicationContext()
                .returnUrl(payPalConfig.getReturnUrl())
                .cancelUrl(payPalConfig.getCancelUrl())
                .brandName("CineZone Booking")
                .landingPage("BILLING")
                .userAction("PAY_NOW");

        orderRequest.applicationContext(applicationContext);

        // Purchase Unit
        List<PurchaseUnitRequest> purchaseUnits = new ArrayList<>();

        PurchaseUnitRequest purchaseUnit = new PurchaseUnitRequest()
                .referenceId(orderId)
                .description("Cinema Ticket Booking - Show #" + request.getShowId())
                .customId(request.getUserId())
                .softDescriptor("CINEMA TICKET")
                .amountWithBreakdown(new AmountWithBreakdown()
                        .currencyCode("USD")
                        .value(totalAmountStr)  // ✅ Use string format for PayPal API
                        .amountBreakdown(new AmountBreakdown()
                                .itemTotal(new Money().currencyCode("USD").value(totalAmountStr))
                        )
                );

        purchaseUnits.add(purchaseUnit);
        orderRequest.purchaseUnits(purchaseUnits);

        // 5. Call PayPal API
        OrdersCreateRequest ordersCreateRequest = new OrdersCreateRequest();
        ordersCreateRequest.requestBody(orderRequest);

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(ordersCreateRequest);
            Order order = response.result();

            log.info("PayPal order created: {}", order.id());

            // 6. Lưu metadata vào Redis
            String paymentSessionKey = "payment_session:" + order.id();
            redisTemplate.opsForValue().set(
                    paymentSessionKey,
                    "PENDING",
                    30,
                    TimeUnit.MINUTES
            );
            String metadataKey = "paypal_metadata:" + order.id();
            Map<String, String> metadata = new HashMap<>();
            metadata.put("orderId", orderId);
            metadata.put("paypalOrderId", order.id());
            metadata.put("showId", String.valueOf(request.getShowId()));
            metadata.put("userId", request.getUserId());
            metadata.put("seats", String.join(",", request.getSeatNumbers()));
            metadata.put("amount", String.valueOf(totalAmountCents)); // ✅ Store as cents (integer)
            metadata.put("amountDisplay", totalAmountStr); // For debugging/display
            metadata.put("paymentMethod", "PAYPAL");

            redisTemplate.opsForHash().putAll(metadataKey, metadata);
            redisTemplate.expire(metadataKey, 30, java.util.concurrent.TimeUnit.MINUTES);

            log.info("Saved metadata to Redis: key={}, amount={} cents", metadataKey, totalAmountCents);

            // 7. Get approval URL
            String approvalUrl = order.links().stream()
                    .filter(link -> "approve".equals(link.rel()))
                    .findFirst()
                    .map(LinkDescription::href)
                    .orElseThrow(() -> new RuntimeException("No approval URL found"));

            log.info("PayPal approval URL: {}", approvalUrl);

            return PaymentCreateResponse.builder()
                    .paymentUrl(approvalUrl)
                    .orderId(order.id())
                    .amount(totalAmountCents) // ✅ Return cents
                    .build();

        } catch (IOException e) {
            log.error("Failed to create PayPal order", e);
            throw new RuntimeException("PayPal order creation failed", e);
        }
    }


    public Map<String, Object> capturePayPalOrder(String paypalOrderId) {
        log.info("Capturing PayPal order: {}", paypalOrderId);

        OrdersCaptureRequest ordersCaptureRequest = new OrdersCaptureRequest(paypalOrderId);
        String paymentSessionKey = "payment_session:" + paypalOrderId;
        String sessionStatus = redisTemplate.opsForValue().get(paymentSessionKey);

        if (sessionStatus == null || "EXPIRED".equals(sessionStatus)) {
            throw new AppException(ErrorCode.SEAT_HOLD_EXPIRED);
        }

        // Get metadata từ Redis
        String metadataKey = "paypal_metadata:" + paypalOrderId;
        Map<Object, Object> rawMetadata = redisTemplate.opsForHash().entries(metadataKey);

        if (rawMetadata.isEmpty()) {
            log.error("Payment metadata not found for paypalOrderId: {}", paypalOrderId);
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        String userId = (String) rawMetadata.get("userId");
        Long showId = Long.parseLong((String) rawMetadata.get("showId"));
        String[] seatNumbers = ((String) rawMetadata.get("seats")).split(",");
        Set<String> seatNumberSet = Set.of(seatNumbers);
        PaymentCreateResponse wsMessage = PaymentCreateResponse.builder()
                .showId(showId)
                .userId(userId)
                .seatNumbers(seatNumberSet)
                .status(SeatInstanceStatus.BOOKED.getStatus())
                .expiresAt(0L)
                .build();

        messagingTemplate.convertAndSend("/topic/show/" + showId, wsMessage);
        try {
            HttpResponse<Order> response = payPalHttpClient.execute(ordersCaptureRequest);
            Order order = response.result();

            log.info("PayPal order captured: {}, Status: {}", order.id(), order.status());

            String amountStr = (String) rawMetadata.get("amount");
            Long amountCents;

            try {
                amountCents = Long.parseLong(amountStr);
                log.info("Parsed amount from metadata: {} cents", amountCents);
            } catch (NumberFormatException e) {
                log.warn("Amount in metadata is decimal format: {}, converting...", amountStr);
                BigDecimal decimalAmount = new BigDecimal(amountStr);
                amountCents = decimalAmount.multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValue();
                log.info("Converted decimal to cents: {} -> {} cents", amountStr, amountCents);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", "COMPLETED".equals(order.status()));
            result.put("orderId", rawMetadata.get("orderId"));
            result.put("paypalOrderId", paypalOrderId);
            result.put("showId", Long.parseLong((String) rawMetadata.get("showId")));
            result.put("userId", rawMetadata.get("userId"));
            result.put("seats", ((String) rawMetadata.get("seats")).split(","));
            result.put("amount", amountCents); // ✅ Return cents (Long)
            result.put("paymentMethod", "PAYPAL");

            try {
                String transactionId = order.purchaseUnits().get(0)
                        .payments()
                        .captures()
                        .get(0)
                        .id();
                result.put("transactionId", transactionId);
            } catch (Exception e) {
                log.warn("Could not extract transaction ID", e);
                result.put("transactionId", paypalOrderId); // Fallback
            }

            // Clean up metadata
            redisTemplate.delete(metadataKey);
            redisTemplate.delete(paymentSessionKey);
            log.info("Deleted metadata from Redis: {}", metadataKey);

            return result;

        } catch (IOException e) {
            log.error("Failed to capture PayPal order: {}", paypalOrderId, e);
            throw new RuntimeException("PayPal capture failed", e);
        }
    }

    // Thêm method refund vào PayPalService.java
    public Map<String, Object> refundPayPalPayment(String transactionId, BigDecimal amount) {
        log.info("Processing PayPal refund - Transaction: {}, Amount: {} cents", transactionId, amount);

        try {

            String refundAmountStr = amount.toString();

            // Build refund request
            RefundRequest refundRequest = new RefundRequest();
            Money money = new Money();
            money.currencyCode("USD");
            money.value(refundAmountStr);
            refundRequest.amount();

            // Create refund
            CapturesRefundRequest capturesRefundRequest = new CapturesRefundRequest(transactionId);
            capturesRefundRequest.requestBody(refundRequest);

            HttpResponse<com.paypal.payments.Refund> response = payPalHttpClient.execute(capturesRefundRequest);
            com.paypal.payments.Refund refund = response.result();

            log.info("PayPal refund successful - Refund ID: {}, Status: {}",
                    refund.id(), refund.status());

            Map<String, Object> result = new HashMap<>();
            result.put("success", "COMPLETED".equals(refund.status()));
            result.put("refundId", refund.id());
            result.put("status", refund.status());
            result.put("amount", amount);

            return result;

        } catch (IOException e) {
            log.error("Failed to refund PayPal payment: {}", transactionId, e);
            throw new RuntimeException("PayPal refund failed: " + e.getMessage(), e);
        }
    }
}