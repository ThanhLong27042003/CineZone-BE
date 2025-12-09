package com.longtapcode.identity_service.repository;
import com.longtapcode.identity_service.entity.Movie;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie,Long> {
    Optional<Movie> findByTitle(String title);
    List<Movie> findTop10ByGenres_NameOrderByIdDesc(String genreName);
    List<Movie> findTop10ByOrderByVoteCountDesc();
    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Movie> searchMovies(@Param("keyword") String keyword);
    @Query("SELECT m FROM User u JOIN u.favoriteMovies m WHERE u.id = :userId")
    List<Movie> findFavoriteMoviesByUserId(@Param("userId") String userId);

    Page<Movie> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
