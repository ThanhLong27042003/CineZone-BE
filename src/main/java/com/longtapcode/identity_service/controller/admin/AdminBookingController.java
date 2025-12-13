package com.longtapcode.identity_service.controller.admin;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.response.*;
import com.longtapcode.identity_service.service.BookingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/bookings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {
    BookingService bookingService;

    @GetMapping("/getAllBookings/{page}/{size}")
    public ApiResponse<Page<BookingResponse>> getAllBookings(
            @PathVariable int page,
            @PathVariable int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Long showId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "bookingDate"));

        Page<BookingResponse> bookings = bookingService.getAllBookingsForAdmin(
                pageable, userId, showId, status, fromDateTime, toDateTime);

        return ApiResponse.<Page<BookingResponse>>builder()
                .result(bookings)
                .build();
    }

    @GetMapping("/{bookingId}")
    public ApiResponse<BookingResponse> getBookingById(@PathVariable Long bookingId) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.getBookingById(bookingId))
                .build();
    }

    @PutMapping("/{bookingId}/cancel")
    public ApiResponse<BookingResponse> cancelBooking(@PathVariable Long bookingId) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.cancelBookingByAdmin(bookingId))
                .message("Booking cancelled successfully")
                .build();
    }

    @PutMapping("/{bookingId}/confirm")
    public ApiResponse<BookingResponse> confirmBooking(@PathVariable Long bookingId) {
        return ApiResponse.<BookingResponse>builder()
                .result(bookingService.confirmBookingByAdmin(bookingId))
                .message("Booking confirmed successfully")
                .build();
    }

    @GetMapping("/statistics")
    public ApiResponse<BookingStatisticsResponse> getStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        return ApiResponse.<BookingStatisticsResponse>builder()
                .result(bookingService.getBookingStatistics(fromDateTime, toDateTime))
                .build();
    }

    @GetMapping("/revenue-by-date")
    public ApiResponse<List<RevenueByDateResponse>> getRevenueByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<Object[]> data = bookingService.getRevenueByDate(fromDateTime, toDateTime);

        List<RevenueByDateResponse> result = data.stream().map(row -> {

            // Convert date
            LocalDate date;
            Object dateObj = row[0];

            if (dateObj instanceof java.sql.Date) {
                date = ((java.sql.Date) dateObj).toLocalDate();
            } else if (dateObj instanceof java.time.LocalDate) {
                date = (LocalDate) dateObj;
            } else {
                throw new IllegalStateException("Unexpected date type: " + dateObj.getClass());
            }

            return RevenueByDateResponse.builder()
                    .date(date)
                    .revenue(((Number) row[1]).longValue())
                    .build();
        }).collect(Collectors.toList());

        return ApiResponse.<List<RevenueByDateResponse>>builder()
                .result(result)
                .build();
    }


    @GetMapping("/top-movies")
    public ApiResponse<List<TopMovieResponse>> getTopMovies(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "10") int limit) {

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : LocalDateTime.now().minusDays(30);
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : LocalDateTime.now();

        List<Object[]> data = bookingService.getTopMovies(fromDateTime, toDateTime, limit);

        List<TopMovieResponse> result = data.stream()
                .map(row -> TopMovieResponse.builder()
                        .title((String) row[0])
                        .bookingCount(((Number) row[1]).longValue())
                        .revenue(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        return ApiResponse.<List<TopMovieResponse>>builder()
                .result(result)
                .build();
    }
}