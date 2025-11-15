package com.longtapcode.identity_service.repository;

import com.longtapcode.identity_service.entity.BookingDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingDetailRepository extends JpaRepository<BookingDetail,Long> {
}
