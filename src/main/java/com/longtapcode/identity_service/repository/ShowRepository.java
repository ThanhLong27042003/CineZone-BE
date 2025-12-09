package com.longtapcode.identity_service.repository;

import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.entity.Show;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show,Long> {
    Optional<Show> findByMovieIDAndShowDateTime(Movie movieID, @NotNull LocalDateTime showDateTime);
    Optional<List<Show>> findByMovieID(Movie movieID);

    Page<Show> findByMovieID(Long movieId, Pageable pageable);
    Page<Show> findByShowDateTime(LocalDateTime dateTime, Pageable pageable);
    Page<Show> findByMovieIDAndShowDateTime(Long movieId, LocalDateTime dateTime, Pageable pageable);

}
