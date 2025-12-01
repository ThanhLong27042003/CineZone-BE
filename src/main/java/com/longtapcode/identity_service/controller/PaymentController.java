package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCallbackResponse;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ApiResponse<PaymentCreateResponse> createPayment(
            @RequestBody PaymentCreateRequest request, HttpServletRequest httpRequest) {

        log.info("Creating payment for user: {}, showId: {}, seats: {}",
                request.getUserId(), request.getShowId(), request.getSeatNumbers());

        PaymentCreateResponse response = paymentService.createPayment(request,httpRequest);

        return ApiResponse.<PaymentCreateResponse>builder()
                .code(1000)
                .message("Payment URL created successfully")
                .result(response)
                .build();
    }

    @GetMapping("/vnpay-callback")
    public ApiResponse<PaymentCallbackResponse> vnpayCallback(
            @RequestBody Map<String, String> params) {

        log.info("Received VNPay callback with params: {}", params);

        PaymentCallbackResponse response = paymentService.processVNPayCallback(params);

        return ApiResponse.<PaymentCallbackResponse>builder()
                .code(1000)
                .message("Booking confirmed successfully")
                .result(response)
                .build();
    }

    @GetMapping("/paypal-callback")
    public ApiResponse<PaymentCallbackResponse> paypalCallback(
            @RequestParam("token") String token,
            @RequestParam(value = "PayerID", required = false) String payerId) {

        log.info("Received PayPal callback - Token: {}, PayerID: {}", token, payerId);

        PaymentCallbackResponse response = paymentService.processPayPalCallback(token, payerId);

        return ApiResponse.<PaymentCallbackResponse>builder()
                .code(1000)
                .message("Booking confirmed successfully")
                .result(response)
                .build();
    }
}