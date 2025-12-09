package com.longtapcode.identity_service.repository;

import com.longtapcode.identity_service.entity.Booking;
import com.longtapcode.identity_service.entity.Show;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
//    @Query("SELECT b FROM Booking b WHERE " +
//            "(:userId IS NULL OR b.id1.id = :userId) AND " +
//            "(:showId IS NULL OR b.showID.id = :showId) AND " +
//            "(:status IS NULL OR b.status = :status) AND " +
//            "(:fromDate IS NULL OR b.bookingDate >= :fromDate) AND " +
//            "(:toDate IS NULL OR b.bookingDate <= :toDate)")
//    Page<Booking> findAllWithFilters(
//            @Param("userId") String userId,
//            @Param("showId") Long showId,
//            @Param("status") String status,
//            @Param("fromDate") LocalDate fromDate,
//            @Param("toDate") LocalDate toDate,
//            Pageable pageable);
//
//    boolean existsByShow(Show show);
//
//    @Query("SELECT COUNT(b) FROM Booking b WHERE b.bookingDate BETWEEN :fromDate AND :toDate")
//    Long countByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
//
//    @Query("SELECT SUM(b.totalPrice) FROM Booking b WHERE b.bookingDate BETWEEN :fromDate AND :toDate")
//    Double sumRevenueByDateRange(@Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);
//
//    @Query("SELECT COUNT(b) FROM Booking b WHERE b.status = :status AND b.bookingDate BETWEEN :fromDate AND :toDate")
//    Long countByStatusAndDateRange(
//            @Param("status") String status,
//            @Param("fromDate") LocalDate fromDate,
//            @Param("toDate") LocalDate toDate);
}
