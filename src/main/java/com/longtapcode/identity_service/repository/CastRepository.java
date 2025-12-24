package com.longtapcode.identity_service.repository;

import com.longtapcode.identity_service.entity.Cast;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CastRepository extends JpaRepository<Cast,Long> {
    @Query("SELECT c FROM Cast c WHERE LOWER(c.name) LIKE LOWER((CONCAT('%',:keyword,'%')))")
    List<Cast> searchCasts(@Param("keyword") String keyword);

    @Query("""
    SELECT DISTINCT c.id
    FROM Movie m
    JOIN m.casts c
    WHERE m.id = :movieId
""")
    List<Long> findCastIdsByMovie(@Param("movieId") Long movieId);

}

