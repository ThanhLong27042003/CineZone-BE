package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.configuration.VNPayConfig;
import com.longtapcode.identity_service.constant.VNPayConstant;
import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.ShowRepository;
import com.longtapcode.identity_service.Util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final ShowRepository showRepository;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Tạo VNPay payment URL
     */
    public PaymentCreateResponse createVNPayPayment(PaymentCreateRequest request, HttpServletRequest httpRequest) {
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_NOT_EXISTED));

        // Calculate amount
        int seatCount = request.getSeatNumbers().size();
        BigDecimal amount = show.getPrice().multiply(BigDecimal.valueOf(seatCount));
        Long totalAmount = amount.longValue();

        String orderId = UUID.randomUUID().toString();
        String txnRef = String.valueOf(System.currentTimeMillis());

        // Build VNPay params
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", VNPayConstant.VERSION);
        vnpParams.put("vnp_Command", VNPayConstant.COMMAND);
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(totalAmount * 100));
        vnpParams.put("vnp_CurrCode", VNPayConstant.CURRENCY_CODE);
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan ve xem phim - " + orderId);
        vnpParams.put("vnp_OrderType", VNPayConstant.ORDER_TYPE);
        vnpParams.put("vnp_Locale", VNPayConstant.LOCALE);
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(httpRequest));

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(calendar.getTime()));

        calendar.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(calendar.getTime()));

        // Save metadata
        String metadataKey = "payment_metadata:" + txnRef;
        Map<String, String> metadata = new HashMap<>();
        metadata.put("orderId", orderId);
        metadata.put("showId", String.valueOf(request.getShowId()));
        metadata.put("userId", request.getUserId());
        metadata.put("seats", String.join(",", request.getSeatNumbers()));
        metadata.put("amount", String.valueOf(totalAmount));
        metadata.put("paymentMethod", "VNPAY");

        redisTemplate.opsForHash().putAll(metadataKey, metadata);
        redisTemplate.expire(metadataKey, 20, java.util.concurrent.TimeUnit.MINUTES);

        // Build URL
        String queryString = VNPayUtil.buildQueryString(vnpParams);
        String secureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), queryString);
        String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

        log.info("Generated VNPay URL for txnRef: {}", txnRef);

        return PaymentCreateResponse.builder()
                .paymentUrl(paymentUrl)
                .orderId(orderId)
                .amount(totalAmount)
                .build();
    }

    /**
     * Verify VNPay callback
     */
    public Map<String, Object> verifyVNPayPayment(Map<String, String> params) {
        if (!VNPayUtil.verifySignature(params, vnPayConfig.getSecretKey())) {
            throw new AppException(ErrorCode.INVALID_SIGNATURE);
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        // Get metadata
        String metadataKey = "payment_metadata:" + txnRef;
        Map<Object, Object> rawMetadata = redisTemplate.opsForHash().entries(metadataKey);

        if (rawMetadata.isEmpty()) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", "00".equals(responseCode));
        result.put("orderId", rawMetadata.get("orderId"));
        result.put("showId", Long.parseLong((String) rawMetadata.get("showId")));
        result.put("userId", rawMetadata.get("userId"));
        result.put("seats", ((String) rawMetadata.get("seats")).split(","));
        result.put("amount", Long.parseLong((String) rawMetadata.get("amount")));
        result.put("paymentMethod", "VNPAY");

        redisTemplate.delete(metadataKey);
        return result;
    }
}