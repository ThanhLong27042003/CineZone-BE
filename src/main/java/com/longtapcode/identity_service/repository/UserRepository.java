package com.longtapcode.identity_service.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.longtapcode.identity_service.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUserName(String userName);

    boolean existsByUserName(String userName);
    boolean existsByIdAndFavoriteMovies_Id(String userId, Long movieId);

}
