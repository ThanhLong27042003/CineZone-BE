package com.longtapcode.identity_service.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.longtapcode.identity_service.dto.response.BookingResponse;
import com.longtapcode.identity_service.service.BookingService;

@WebMvcTest(BookingController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for unit test
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        bookingResponse = BookingResponse.builder()
                .id(1L)
                .userId("user-123")
                .status("CONFIRMED")
                .bookingDate(LocalDateTime.now())
                .build();
    }

    private void mockSecurityContext() {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(jwt.getClaimAsString("userId")).thenReturn("user-123");

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getBookingById_Success() throws Exception {
        when(bookingService.getBookingById(anyLong())).thenReturn(bookingResponse);

        mockMvc.perform(get("/booking/1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"));
    }

    @Test
    void getMyBookings_Success() throws Exception {
        mockSecurityContext();
        when(bookingService.getBookingsByUserId(eq("user-123"))).thenReturn(List.of(bookingResponse));

        mockMvc.perform(get("/booking/my-bookings").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].id").value(1))
                .andExpect(jsonPath("$.result[0].userId").value("user-123"));
    }

    @Test
    void cancelBooking_Success() throws Exception {
        mockSecurityContext();
        Map<String, Object> mockRefundResult = Map.of(
                "success", true,
                "refundId", "REF-123",
                "message", "Refund processed");

        when(bookingService.cancelMyBookingWithRefund(eq(1L))).thenReturn(mockRefundResult);

        mockMvc.perform(put("/booking/1/cancel").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.success").value(true))
                .andExpect(jsonPath("$.result.refundId").value("REF-123"));
    }
}
