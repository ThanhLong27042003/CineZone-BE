package com.longtapcode.identity_service.repository;

import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.Show;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
@Query("SELECT b FROM Booking b WHERE " +
        "(:userId IS NULL OR b.id1.id = :userId) AND " +
        "(:showId IS NULL OR b.showID.id = :showId) AND " +
        "(:status IS NULL OR b.status = :status) AND " +
        "(:fromDate IS NULL OR b.bookingDate >= :fromDate) AND " +
        "(:toDate IS NULL OR b.bookingDate <= :toDate)")
Page<Booking> findAllWithFilters(
        @Param("userId") String userId,
        @Param("showId") Long showId,
        @Param("status") String status,
        @Param("fromDate") LocalDateTime fromDate,
        @Param("toDate") LocalDateTime toDate,
        Pageable pageable);

    boolean existsByShowID(Show show);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate BETWEEN :fromDate AND :toDate")
    Long countByDateRange(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b WHERE b.bookingDate BETWEEN :fromDate AND :toDate AND b.status = 'CONFIRMED'")
    Long sumRevenueByDateRange(@Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status AND b.bookingDate BETWEEN :fromDate AND :toDate")
    Long countByStatusAndDateRange(
            @Param("status") String status,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    // Revenue by date for charts
    @Query("SELECT function('date', b.bookingDate) as d, COALESCE(SUM(b.totalPrice), 0) as revenue " +
            "FROM Booking b " +
            "WHERE b.bookingDate BETWEEN :fromDate AND :toDate AND b.status = 'CONFIRMED' " +
            "GROUP BY function('date', b.bookingDate) " +
            "ORDER BY function('date', b.bookingDate)")
    List<Object[]> getRevenueByDate(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate);

    // Top movies by bookings
    @Query("SELECT m.title, COUNT(b.id) as bookingCount, COALESCE(SUM(b.totalPrice), 0) as revenue " +
            "FROM Booking b " +
            "JOIN b.showID s " +
            "JOIN s.movieID m " +
            "WHERE b.bookingDate BETWEEN :fromDate AND :toDate AND b.status = 'CONFIRMED' " +
            "GROUP BY m.id, m.title " +
            "ORDER BY bookingCount DESC")
    List<Object[]> getTopMoviesByBookings(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("limit") int limit);
}
