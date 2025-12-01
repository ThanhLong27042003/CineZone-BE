package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.configuration.PayPalConfig;
import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.ShowRepository;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.orders.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayPalService {

    private final PayPalHttpClient payPalHttpClient;
    private final PayPalConfig payPalConfig;
    private final ShowRepository showRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Tạo PayPal Order
     */
    public PaymentCreateResponse createPayPalOrder(PaymentCreateRequest request) {
        log.info("Creating PayPal order for showId: {}, user: {}", request.getShowId(), request.getUserId());

        // 1. Get show info
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        // 2. Calculate amount
        int seatCount = request.getSeatNumbers().size();
        BigDecimal pricePerSeat = show.getPrice();
        BigDecimal totalAmount = pricePerSeat.multiply(BigDecimal.valueOf(seatCount));

        // ✅ PayPal yêu cầu format: "10.00" (2 decimal places)
        String totalAmountStr = totalAmount.setScale(2, RoundingMode.HALF_UP).toString();

        // ✅ Convert to cents/integer for storage (avoid decimal parsing issues)
        Long totalAmountCents = totalAmount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        log.info("PayPal order amount: ${} ({} cents)", totalAmountStr, totalAmountCents);

        // 3. Generate orderId để lưu metadata
        String orderId = UUID.randomUUID().toString();

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

        // Items detail
        List<Item> items = new ArrayList<>();
        String seatList = String.join(", ", request.getSeatNumbers());
        Item item = new Item()
                .name("Movie Tickets")
                .description("Seats: " + seatList)
                .unitAmount(new Money().currencyCode("USD").value(pricePerSeat.setScale(2, RoundingMode.HALF_UP).toString()))
                .quantity(String.valueOf(seatCount))
                .category("DIGITAL_GOODS");
        items.add(item);

        purchaseUnit.items(items);
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

    /**
     * Capture PayPal Payment sau khi user approve
     */
    public Map<String, Object> capturePayPalOrder(String paypalOrderId) {
        log.info("Capturing PayPal order: {}", paypalOrderId);

        OrdersCaptureRequest ordersCaptureRequest = new OrdersCaptureRequest(paypalOrderId);

        try {
            HttpResponse<Order> response = payPalHttpClient.execute(ordersCaptureRequest);
            Order order = response.result();

            log.info("PayPal order captured: {}, Status: {}", order.id(), order.status());

            // Get metadata từ Redis
            String metadataKey = "paypal_metadata:" + paypalOrderId;
            Map<Object, Object> rawMetadata = redisTemplate.opsForHash().entries(metadataKey);

            if (rawMetadata.isEmpty()) {
                log.error("Payment metadata not found for paypalOrderId: {}", paypalOrderId);
                throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
            }

            // ✅ Parse amount safely (now it's integer/cents)
            String amountStr = (String) rawMetadata.get("amount");
            Long amountCents;

            try {
                // Try to parse as Long directly
                amountCents = Long.parseLong(amountStr);
                log.info("Parsed amount from metadata: {} cents", amountCents);
            } catch (NumberFormatException e) {
                // Fallback: if somehow still decimal format, convert it
                log.warn("Amount in metadata is decimal format: {}, converting...", amountStr);
                BigDecimal decimalAmount = new BigDecimal(amountStr);
                amountCents = decimalAmount.multiply(BigDecimal.valueOf(100))
                        .setScale(0, RoundingMode.HALF_UP)
                        .longValue();
                log.info("Converted decimal to cents: {} -> {} cents", amountStr, amountCents);
            }

            // Return data để PaymentService xử lý
            Map<String, Object> result = new HashMap<>();
            result.put("success", "COMPLETED".equals(order.status()));
            result.put("orderId", rawMetadata.get("orderId"));
            result.put("paypalOrderId", paypalOrderId);
            result.put("showId", Long.parseLong((String) rawMetadata.get("showId")));
            result.put("userId", rawMetadata.get("userId"));
            result.put("seats", ((String) rawMetadata.get("seats")).split(","));
            result.put("amount", amountCents); // ✅ Return cents (Long)
            result.put("paymentMethod", "PAYPAL");

            // Get transaction ID safely
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
            log.info("Deleted metadata from Redis: {}", metadataKey);

            return result;

        } catch (IOException e) {
            log.error("Failed to capture PayPal order: {}", paypalOrderId, e);
            throw new RuntimeException("PayPal capture failed", e);
        }
    }
}