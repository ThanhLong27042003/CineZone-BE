package com.longtapcode.identity_service.repository;

import com.longtapcode.identity_service.entity.Genre;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {
    @Query("""
    SELECT DISTINCT g.id
    FROM Movie m
    JOIN m.genres g
    WHERE m.id = :movieId
""")
    List<Long> findGenreIdsByMovie(@Param("movieId") Long movieId);

}
