package com.longtapcode.identity_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.longtapcode.identity_service.entity.SeatInstance;

@Repository
public interface seatInstanceRepository extends JpaRepository<SeatInstance, Long> {}
