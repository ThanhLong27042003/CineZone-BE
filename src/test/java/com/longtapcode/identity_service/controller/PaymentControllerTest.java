package com.longtapcode.identity_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCallbackResponse;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.service.PaymentService;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simple unit test
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    private PaymentCreateRequest createRequest;
    private PaymentCreateResponse createResponse;
    private PaymentCallbackResponse callbackResponse;

    @BeforeEach
    void setUp() {
        createRequest = new PaymentCreateRequest();
        createRequest.setUserId("user-123");
        createRequest.setShowId(1L);
        createRequest.setSeatNumbers(Set.of("A1", "A2"));
        createRequest.setAmount(new BigDecimal("200000"));
        createRequest.setPaymentMethod("PAYPAL");

        createResponse = PaymentCreateResponse.builder()
                .paymentUrl("http://sandbox.paypal.com/checkout?token=123")
                .orderId("ORDER-123")
                .build();

        callbackResponse = PaymentCallbackResponse.builder()
                .success(true)
                .bookingId(1L)
                .orderId("ORDER-123")
                .message("Booking confirmed")
                .build();
    }

    @Test
    void createPayment_Success() throws Exception {
        when(paymentService.createPayment(any(), any())).thenReturn(createResponse);

        mockMvc.perform(post("/payment/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.paymentUrl").value("http://sandbox.paypal.com/checkout?token=123"))
                .andExpect(jsonPath("$.result.orderId").value("ORDER-123"));
    }

    @Test
    void paypalCallback_Success() throws Exception {
        when(paymentService.processPayPalCallback(anyString(), anyString())).thenReturn(callbackResponse);

        mockMvc.perform(get("/payment/paypal-callback")
                        .param("token", "123")
                        .param("PayerID", "payer-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.success").value(true))
                .andExpect(jsonPath("$.result.bookingId").value(1))
                .andExpect(jsonPath("$.message").value("Booking confirmed successfully"));
    }
}
