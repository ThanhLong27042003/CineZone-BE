package com.longtapcode.identity_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

import com.longtapcode.identity_service.dto.response.BookingResponse;
import com.longtapcode.identity_service.dto.response.BookingStatisticsResponse;
import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.BookingMapper;
import com.longtapcode.identity_service.repository.BookingRepository;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private BookingCancellationService cancellationService;

    @InjectMocks
    private BookingService bookingService;

    private Booking booking;
    private User user;
    private Show show;
    private BookingResponse bookingResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-123");

        show = new Show();
        show.setId(1L);
        show.setShowDateTime(LocalDateTime.now().plusDays(1)); // future show

        booking = Booking.builder()
                .id(1L)
                .id1(user)
                .showID(show)
                .status("CONFIRMED")
                .totalPrice(new BigDecimal("100000"))
                .build();

        bookingResponse = BookingResponse.builder()
                .id(1L)
                .userId("user-123")
                .status("CONFIRMED")
                .build();
    }

    @Nested
    @DisplayName("cancelBookingByUser")
    class CancelBookingByUser {

        @Test
        @DisplayName("Success - Should cancel booking if > 2 hours before showtime")
        void cancelBookingByUser_Success() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
            when(bookingMapper.toBookingResponse(any())).thenReturn(bookingResponse);

            BookingResponse result = bookingService.cancelBookingByUser(1L, "user-123");

            assertNotNull(result);
            assertEquals("CANCELLED", booking.getStatus());
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("Fail - User does not own the booking")
        void cancelBookingByUser_Fail_Unauthorized() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            AppException ex =
                    assertThrows(AppException.class, () -> bookingService.cancelBookingByUser(1L, "different-user"));

            assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Fail - Booking is already cancelled")
        void cancelBookingByUser_Fail_AlreadyCancelled() {
            booking.setStatus("CANCELLED");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            AppException ex =
                    assertThrows(AppException.class, () -> bookingService.cancelBookingByUser(1L, "user-123"));

            assertEquals(ErrorCode.BOOKING_ALREADY_CANCELLED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Fail - Less than 2 hours before showtime")
        void cancelBookingByUser_Fail_TooLate() {
            show.setShowDateTime(LocalDateTime.now().plusHours(1)); // only 1 hour left
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

            AppException ex =
                    assertThrows(AppException.class, () -> bookingService.cancelBookingByUser(1L, "user-123"));

            assertEquals(ErrorCode.CANNOT_CANCEL_BOOKING, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Admin Actions")
    class AdminActions {

        @Test
        @DisplayName("Success - Admin cancels booking")
        void cancelBookingByAdmin_Success() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenReturn(booking);
            when(bookingMapper.toBookingResponse(any())).thenReturn(bookingResponse);

            assertDoesNotThrow(() -> bookingService.cancelBookingByAdmin(1L));

            assertEquals("CANCELLED", booking.getStatus());
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("Success - Admin confirms booking")
        void confirmBookingByAdmin_Success() {
            booking.setStatus("PENDING");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(any())).thenReturn(booking);
            when(bookingMapper.toBookingResponse(any())).thenReturn(bookingResponse);

            assertDoesNotThrow(() -> bookingService.confirmBookingByAdmin(1L));

            assertEquals("CONFIRMED", booking.getStatus());
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("Success - Get booking statistics")
        void getBookingStatistics_Success() {
            LocalDateTime now = LocalDateTime.now();
            when(bookingRepository.countByDateRange(any(), any())).thenReturn(100L);
            when(bookingRepository.sumRevenueByDateRange(any(), any())).thenReturn(500000L);
            when(bookingRepository.countByStatusAndDateRange(eq("CONFIRMED"), any(), any()))
                    .thenReturn(80L);
            when(bookingRepository.countByStatusAndDateRange(eq("CANCELLED"), any(), any()))
                    .thenReturn(10L);
            when(bookingRepository.countByStatusAndDateRange(eq("PENDING"), any(), any()))
                    .thenReturn(10L);

            BookingStatisticsResponse stats = bookingService.getBookingStatistics(now.minusDays(7), now);

            assertEquals(100L, stats.getTotalBookings());
            assertEquals(500000L, stats.getTotalRevenue());
            assertEquals(80L, stats.getConfirmedBookings());
        }
    }

    @Nested
    @DisplayName("cancelMyBookingWithRefund")
    class CancelMyBookingWithRefund {
        @Test
        @DisplayName("Success - delegates to cancellation service")
        void cancelMyBookingWithRefund_Success() {
            Map<String, Object> expectedResponse = Map.of("success", true);
            when(cancellationService.cancelBookingWithRefund(1L)).thenReturn(expectedResponse);

            Map<String, Object> result = bookingService.cancelMyBookingWithRefund(1L);

            assertEquals(expectedResponse, result);
            verify(cancellationService).cancelBookingWithRefund(1L);
        }
    }
}
