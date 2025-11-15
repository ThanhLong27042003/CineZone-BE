package com.longtapcode.identity_service.repository;

import com.longtapcode.identity_service.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    Optional<List<Seat>> findByVip(int vip);
}
