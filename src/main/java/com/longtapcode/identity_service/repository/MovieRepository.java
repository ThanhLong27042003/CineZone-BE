package com.longtapcode.identity_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.longtapcode.identity_service.entity.Movie;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTitle(String title);

    // Thay thế findTop10ByOrderByVoteCountDesc
    @Query("SELECT DISTINCT m FROM Movie m " + "LEFT JOIN FETCH m.genres "
            + "LEFT JOIN FETCH m.casts "
            + "ORDER BY m.voteCount DESC")
    List<Movie> findTop10WithGenresAndCastsOrderByVoteCountDesc(Pageable pageable);

    // Thay thế findTop10ByGenres_NameOrderByIdDesc
    @Query("SELECT DISTINCT m FROM Movie m " + "LEFT JOIN FETCH m.genres g "
            + "LEFT JOIN FETCH m.casts "
            + "WHERE g.name = :genreName "
            + "ORDER BY m.id DESC")
    List<Movie> findTop10WithGenresAndCastsByGenreName(@Param("genreName") String genreName, Pageable pageable);

    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Movie> searchMovies(@Param("keyword") String keyword);

    @Query("SELECT m FROM User u JOIN u.favoriteMovies m WHERE u.id = :userId")
    List<Movie> findFavoriteMoviesByUserId(@Param("userId") String userId);

    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
