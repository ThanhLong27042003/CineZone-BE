package com.longtapcode.identity_service.repository;

import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Show;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show,Long> {
    Optional<List<Show>> findByMovieID(Movie movieID);
    Page<Show> findByMovieID(Long movieId, Pageable pageable);
    Page<Show> findByShowDateTime(LocalDateTime dateTime, Pageable pageable);
    Page<Show> findByMovieIDAndShowDateTime(Long movieId, LocalDateTime dateTime, Pageable pageable);

    @Query(value = """
        SELECT COUNT(*)
        FROM shows s
        JOIN movies m ON s.movieid = m.movieid
        WHERE s.room_id = :roomId
          AND (:excludeShowId IS NULL OR s.showid <> :excludeShowId)
          AND s.show_date_time < :endDateTime
          AND TIMESTAMPADD(MINUTE, m.runtime, s.show_date_time) > :startDateTime
        """, nativeQuery = true)

    Long countOverlappingShow(
            @Param("roomId") Long roomId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("excludeShowId") Long excludeShowId
    );

    @Query("""
        SELECT s FROM Show s
        WHERE s.roomId.roomId = :roomId
        AND s.showDateTime BETWEEN :startDate AND :endDate
        ORDER BY s.showDateTime ASC
    """)
    List<Show> findByRoomAndDateRange(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
