package com.longtapcode.identity_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.BookingDetail;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.BookingDetailRepository;
import com.longtapcode.identity_service.repository.BookingRepository;

@ExtendWith(MockitoExtension.class)
class BookingCancellationServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDetailRepository bookingDetailRepository;

    @Mock
    private PayPalService payPalService;

    @Mock
    private VNPayService vnPayService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private BookingCancellationService cancellationService;

    private Booking booking;
    private Show show;

    @BeforeEach
    void setUp() {
        show = new Show();
        show.setId(1L);
        show.setShowDateTime(LocalDateTime.now().plusDays(1)); // More than 2 hours in the future

        User user = new User();
        user.setId("user-123");

        booking = Booking.builder()
                .id(1L)
                .showID(show)
                .id1(user)
                .status("CONFIRMED")
                .paymentMethod("PAYPAL")
                .transactionId("PAYPAL-TXN-123")
                .totalPrice(new BigDecimal("150000"))
                .build();
    }

    @Nested
    @DisplayName("cancelBookingWithRefund")
    class CancelBookingWithRefund {

        @Test
        @DisplayName("Success - PayPal Refund")
        void cancelWithRefund_Success_PayPal() {
            // Mock booking details for releasing seats
            BookingDetail detail =
                    BookingDetail.builder().bookingID(booking).seatNumber("A1").build();

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            when(payPalService.refundPayPalPayment("PAYPAL-TXN-123", new BigDecimal("150000")))
                    .thenReturn(Map.of("refundId", "REF-123", "amount", new BigDecimal("150000")));
            when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));

            Map<String, Object> result = cancellationService.cancelBookingWithRefund(1L);

            assertTrue((Boolean) result.get("success"));
            assertEquals("REF-123", result.get("refundId"));
            assertEquals("CANCELLED", booking.getStatus());

            verify(bookingRepository).save(booking);
            verify(redisTemplate).delete("booked:1:A1");
            verify(messagingTemplate).convertAndSend(eq("/topic/show/1"), any(Object.class));
        }

        @Test
        @DisplayName("Success - VNPay Refund (Manual logic)")
        void cancelWithRefund_Success_VNPay() {
            booking.setPaymentMethod("VNPAY");

            BookingDetail detail =
                    BookingDetail.builder().bookingID(booking).seatNumber("A2").build();

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            when(bookingDetailRepository.findAll()).thenReturn(List.of(detail));

            Map<String, Object> result = cancellationService.cancelBookingWithRefund(1L);

            assertTrue((Boolean) result.get("success"));
            assertEquals("MANUAL_VNPAY", result.get("refundId"));
            assertEquals("CANCELLED", booking.getStatus());

            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("Fail - Unsupported payment method")
        void cancelWithRefund_Fail_UnsupportedPayment() {
            booking.setPaymentMethod("UNKNOWN");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            RuntimeException ex =
                    assertThrows(RuntimeException.class, () -> cancellationService.cancelBookingWithRefund(1L));

            assertTrue(ex.getMessage().contains("Unsupported payment method"));
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Fail - Less than 2 hours before show")
        void cancelWithRefund_Fail_TooLate() {
            show.setShowDateTime(LocalDateTime.now().plusHours(1)); // Only 1 hour left
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            RuntimeException ex =
                    assertThrows(RuntimeException.class, () -> cancellationService.cancelBookingWithRefund(1L));

            assertTrue(ex.getMessage().contains("Cannot cancel booking less than 2 hours"));
        }

        @Test
        @DisplayName("Fail - Booking already cancelled")
        void cancelWithRefund_Fail_AlreadyCancelled() {
            booking.setStatus("CANCELLED");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            AppException ex = assertThrows(AppException.class, () -> cancellationService.cancelBookingWithRefund(1L));

            assertEquals(ErrorCode.BOOKING_ALREADY_CANCELLED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Fail - PayPal Missing Transaction ID")
        void cancelWithRefund_Fail_PayPalMissingTxnId() {
            booking.setTransactionId(null);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            RuntimeException ex =
                    assertThrows(RuntimeException.class, () -> cancellationService.cancelBookingWithRefund(1L));

            assertTrue(ex.getMessage().contains("Transaction ID not found"));
        }
    }
}
