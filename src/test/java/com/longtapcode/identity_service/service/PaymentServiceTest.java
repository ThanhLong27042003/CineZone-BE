package com.longtapcode.identity_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.longtapcode.identity_service.dto.request.PaymentCreateRequest;
import com.longtapcode.identity_service.dto.response.PaymentCallbackResponse;
import com.longtapcode.identity_service.dto.response.PaymentCreateResponse;
import com.longtapcode.identity_service.dto.response.SeatResponse;
import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Room;
import com.longtapcode.identity_service.entity.Show;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.BookingDetailRepository;
import com.longtapcode.identity_service.repository.BookingRepository;
import com.longtapcode.identity_service.repository.ShowRepository;
import com.longtapcode.identity_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShowRepository showRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDetailRepository bookingDetailRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private VNPayService vnPayService;

    @Mock
    private PayPalService payPalService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private SeatService seatService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Show show;
    private Booking booking;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-123");
        user.setEmailAddress("test@example.com");
        user.setUserName("testUser");

        Movie movie = new Movie();
        movie.setTitle("Test Movie");

        Room room = new Room();
        room.setName("Room 1");

        show = new Show();
        show.setId(1L);
        show.setMovieID(movie);
        show.setRoomId(room);
        show.setShowDateTime(LocalDateTime.now().plusDays(1));
        show.setPrice(new BigDecimal("100000"));

        booking = Booking.builder()
                .id(1L)
                .id1(user)
                .showID(show)
                .orderId("ORDER-123")
                .paymentMethod("PAYPAL")
                .status("PENDING")
                .bookingDate(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @BeforeEach
        void setupRedisMocks() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        }

        @Test
        @DisplayName("Success - Create PayPal Payment")
        void createPayment_Success_PayPal() {
            PaymentCreateRequest request = new PaymentCreateRequest();
            request.setUserId("user-123");
            request.setShowId(1L);
            request.setPaymentMethod("PAYPAL");
            request.setAmount(new BigDecimal("150000"));
            request.setSeatNumbers(Set.of("A1"));

            when(valueOperations.get("hold:1:A1")).thenReturn("user-123");
            when(showRepository.findById(1L)).thenReturn(Optional.of(show));
            when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
            when(payPalService.createPayPalOrder(any(), anyString()))
                    .thenReturn(PaymentCreateResponse.builder()
                            .orderId("PAYPAL-ORD")
                            .build());

            assertDoesNotThrow(() -> paymentService.createPayment(request, httpRequest));

            verify(bookingRepository).save(any(Booking.class));
            verify(payPalService).createPayPalOrder(any(), anyString());
            verify(hashOperations).put(eq("paypal_metadata:PAYPAL-ORD"), eq("bookingId"), anyString());
        }

        @Test
        @DisplayName("Fail - Seats not held by user")
        void createPayment_Fail_SeatsNotHeld() {
            PaymentCreateRequest request = new PaymentCreateRequest();
            request.setUserId("user-123");
            request.setShowId(1L);
            request.setSeatNumbers(Set.of("A1"));

            when(valueOperations.get("hold:1:A1")).thenReturn("other-user");

            AppException ex =
                    assertThrows(AppException.class, () -> paymentService.createPayment(request, httpRequest));

            assertEquals(ErrorCode.SEAT_NOT_AVAILABLE, ex.getErrorCode());
        }

        @Test
        @DisplayName("Fail - Show already passed")
        void createPayment_Fail_ShowPassed() {
            show.setShowDateTime(LocalDateTime.now().minusDays(1));
            PaymentCreateRequest request = new PaymentCreateRequest();
            request.setUserId("user-123");
            request.setShowId(1L);
            request.setSeatNumbers(Set.of("A1"));

            when(valueOperations.get("hold:1:A1")).thenReturn("user-123");
            when(showRepository.findById(1L)).thenReturn(Optional.of(show));

            AppException ex =
                    assertThrows(AppException.class, () -> paymentService.createPayment(request, httpRequest));

            assertEquals(ErrorCode.SHOW_ALREADY_PASSED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Process Callbacks")
    class ProcessCallbacks {

        @BeforeEach
        void setupRedisMocks() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        }

        @Test
        @DisplayName("Success - processPayPalCallback")
        void processPayPalCallback_Success() {
            Map<String, Object> paymentResult = new HashMap<>();
            paymentResult.put("success", true);
            paymentResult.put("orderId", "ORDER-123");
            paymentResult.put("transactionId", "TXN-123");
            paymentResult.put("showId", 1L);
            paymentResult.put("userId", "user-123");
            paymentResult.put("seats", new String[] {"A1"});
            paymentResult.put("amount", 100000L); // In cents for PayPal? Assuming based on cast

            when(payPalService.capturePayPalOrder("PAYPAL-ORD")).thenReturn(paymentResult);
            when(bookingRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(booking));

            SeatResponse seatResponse = new SeatResponse(1, "A1", 0);
            when(seatService.getSeatBySeatNumber("A1")).thenReturn(seatResponse);

            PaymentCallbackResponse response = paymentService.processPayPalCallback("PAYPAL-ORD", "token123");

            assertTrue(response.getSuccess());
            assertEquals("CONFIRMED", booking.getStatus());
            verify(bookingRepository).save(booking);
            verify(bookingDetailRepository).saveAll(anyIterable());
            verify(kafkaProducerService).publishBookingConfirmed(any());
        }

        @Test
        @DisplayName("Fail - Payment failed logic")
        void processPayPalCallback_Fail_PaymentFailed() {
            Map<String, Object> paymentResult = new HashMap<>();
            paymentResult.put("success", false);
            paymentResult.put("orderId", "ORDER-123");
            paymentResult.put("showId", 1L);
            paymentResult.put("userId", "user-123");
            paymentResult.put("seats", new String[] {"A1"});

            when(payPalService.capturePayPalOrder("PAYPAL-ORD")).thenReturn(paymentResult);
            when(bookingRepository.findByOrderId("ORDER-123")).thenReturn(Optional.of(booking));
            when(valueOperations.get("hold:1:A1")).thenReturn("user-123");

            AppException ex = assertThrows(
                    AppException.class, () -> paymentService.processPayPalCallback("PAYPAL-ORD", "token123"));

            assertEquals(ErrorCode.PAYMENT_FAILED, ex.getErrorCode());
            assertEquals("CANCELLED", booking.getStatus());
            verify(redisTemplate).delete("hold:1:A1");
            verify(messagingTemplate).convertAndSend(eq("/topic/show/1"), any(Object.class));
        }
    }

    @Nested
    @DisplayName("Scheduled Tasks")
    class ScheduledTasks {

        @Test
        @DisplayName("Success - cancelExpiredPaymentProcessing")
        void cancelExpiredPaymentProcessing_Success() {
            Booking expiredBooking = new Booking();
            expiredBooking.setId(1L);
            expiredBooking.setStatus("PAYMENT_PROCESSING");

            when(bookingRepository.findByStatusAndBookingDateBefore(eq("PAYMENT_PROCESSING"), any()))
                    .thenReturn(List.of(expiredBooking));

            paymentService.cancelExpiredPaymentProcessing();

            assertEquals("EXPIRED", expiredBooking.getStatus());
            verify(bookingRepository).save(expiredBooking);
        }
    }
}
