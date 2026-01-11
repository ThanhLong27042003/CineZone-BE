
package com.longtapcode.identity_service.service;

import com.longtapcode.identity_service.dto.response.BookingResponse;
import com.longtapcode.identity_service.dto.response.BookingStatisticsResponse;
import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.BookingMapper;
import com.longtapcode.identity_service.repository.BookingRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BookingService {
    BookingRepository bookingRepository;
    BookingMapper bookingMapper;
    @PreAuthorize("hasRole('ADMIN')")
    public Page<BookingResponse> getAllBookingsForAdmin(
            Pageable pageable,
            String userId,
            Long showId,
            String status,
            LocalDateTime fromDate,
            LocalDateTime toDate) {

        Page<Booking> bookings = bookingRepository.findAllWithFilters(
                userId, showId, status, fromDate, toDate, pageable);

        return bookings.map(bookingMapper::toBookingResponse);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));
        return bookingMapper.toBookingResponse(booking);
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public BookingResponse cancelBookingByAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if ("CANCELLED".equals(booking.getStatus())) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CANCELLED);
        }

        booking.setStatus("CANCELLED");

        log.info("Admin cancelled booking: {}", bookingId);

        return bookingMapper.toBookingResponse(bookingRepository.save(booking));
    }
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public BookingResponse confirmBookingByAdmin(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new AppException(ErrorCode.BOOKING_NOT_FOUND));

        if ("CONFIRMED".equals(booking.getStatus())) {
            throw new AppException(ErrorCode.BOOKING_ALREADY_CONFIRMED);
        }

        booking.setStatus("CONFIRMED");

        log.info("Admin confirmed booking: {}", bookingId);

        return bookingMapper.toBookingResponse(bookingRepository.save(booking));
    }
    @PreAuthorize("hasRole('ADMIN')")
    public BookingStatisticsResponse getBookingStatistics(LocalDateTime fromDate, LocalDateTime toDate) {
        Long totalBookings = bookingRepository.countByDateRange(fromDate, toDate);
        Long totalRevenue = bookingRepository.sumRevenueByDateRange(fromDate, toDate);
        Long confirmedBookings = bookingRepository.countByStatusAndDateRange("CONFIRMED", fromDate, toDate);
        Long cancelledBookings = bookingRepository.countByStatusAndDateRange("CANCELLED", fromDate, toDate);
        Long pendingBookings = bookingRepository.countByStatusAndDateRange("PENDING", fromDate, toDate);

        return BookingStatisticsResponse.builder()
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0L)
                .confirmedBookings(confirmedBookings)
                .cancelledBookings(cancelledBookings)
                .pendingBookings(pendingBookings)
                .build();
    }

    public List<Object[]> getRevenueByDate(LocalDateTime fromDate, LocalDateTime toDate) {
        return bookingRepository.getRevenueByDate(fromDate, toDate);
    }

    public List<Object[]> getTopMovies(LocalDateTime fromDate, LocalDateTime toDate, int limit) {
        return bookingRepository.getTopMoviesByBookings(fromDate, toDate, limit);
    }
}
