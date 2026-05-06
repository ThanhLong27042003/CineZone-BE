package com.longtapcode.identity_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.longtapcode.identity_service.entity.Seat;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    Optional<List<Seat>> findByVip(int vip);

    Optional<Seat> findBySeatNumber(String seatNumber);
}
