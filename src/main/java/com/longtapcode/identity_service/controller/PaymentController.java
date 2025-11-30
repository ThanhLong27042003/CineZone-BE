package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCallbackResponse;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.service.PaymentService;
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
            @RequestBody PaymentCreateRequest request) {

        log.info("Creating payment for user: {}, showId: {}, seats: {}",
                request.getUserId(), request.getShowId(), request.getSeatNumbers());

        PaymentCreateResponse response = paymentService.createPayment(request);

        return ApiResponse.<PaymentCreateResponse>builder()
                .code(1000)
                .message("Payment URL created successfully")
                .result(response)
                .build();
    }

    @PostMapping("/callback")
    public ApiResponse<PaymentCallbackResponse> handlePaymentCallback(
            @RequestBody Map<String, String> params) {

        log.info("Received payment callback: {}", params);

        PaymentCallbackResponse response = paymentService.processPaymentCallback(params);

        return ApiResponse.<PaymentCallbackResponse>builder()
                .code(1000)
                .message("Booking confirmed successfully")
                .result(response)
                .build();
    }
}