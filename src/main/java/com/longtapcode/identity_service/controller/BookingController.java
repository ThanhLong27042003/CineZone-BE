package com.longtapcode.identity_service.controller;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.response.BookingResponse;
import com.longtapcode.identity_service.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/booking")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingController {
    BookingService bookingService;

    /**
     * Lấy danh sách booking của user đang đăng nhập
     */
    @GetMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<BookingResponse>> getMyBookings() {
        var context = SecurityContextHolder.getContext();
        Jwt jwt = (Jwt) context.getAuthentication().getPrincipal();
        String userId = jwt.getClaimAsString("userId");

        return ApiResponse.<List<BookingResponse>>builder()
                .result(bookingService.getBookingsByUserId(userId))
                .build();
    }

    /**
     * Lấy chi tiết 1 booking
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BookingResponse> getBookingDetail(@PathVariable Long bookingId) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.getBookingById(bookingId))
                .build();
    }

    /**
     * Hủy booking (chỉ cho phép nếu chưa quá giờ chiếu)
     */
//    @PutMapping("/{bookingId}/cancel")
//    @PreAuthorize("isAuthenticated()")
//    public ApiResponse<BookingResponse> cancelBooking(@PathVariable Long bookingId) {
//        var context = SecurityContextHolder.getContext();
//        Jwt jwt = (Jwt) context.getAuthentication().getPrincipal();
//        String userId = jwt.getClaimAsString("userId");
//        return ApiResponse.<BookingResponse>builder()
//                .result(bookingService.cancelBookingByUser(bookingId, userId))
//                .message("Booking cancelled successfully")
//                .build();
//    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, Object>> cancelBooking(
            @PathVariable Long bookingId) {

        Map<String, Object> result = bookingService.cancelMyBookingWithRefund(bookingId);

        return ApiResponse.<Map<String, Object>>builder()
                .result(result)
                .build();
    }
}