package com.longtapcode.identity_service.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.longtapcode.identity_service.dto.request.MovieRequest;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.repository.MovieRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.longtapcode.identity_service.constant.PredefinedRole;
import com.longtapcode.identity_service.dto.request.CreationUserRequest;
import com.longtapcode.identity_service.dto.request.UpdateUserRequest;
import com.longtapcode.identity_service.dto.response.UserResponse;
import com.longtapcode.identity_service.entity.Role;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.UserMapper;
import com.longtapcode.identity_service.repository.UserRepository;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    private final MovieRepository movieRepository;

    public void createUserService(CreationUserRequest request) {
        if (userRepository.existsByUserName(request.getUserName())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        String passwordEncoded = passwordEncoder.encode(request.getPassword());
        request.setPassword(passwordEncoded);
        User user = userMapper.toUser(request);
        Set<Role> roles = new HashSet<>();
        Role role = new Role();
        role.setName(PredefinedRole.USER_ROLE);
        roles.add(role);
        user.setRoles(roles);
        userRepository.save(user);
    }

    public void updateUserService(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        userRepository.save(user);
    }

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return userMapper.toListUserResponse(users);
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new AppException(ErrorCode.USER_NOT_EXISTED);
        }
        userRepository.deleteById(id);
    }
    public void addFavoriteMovie(String userId, Long movieId){
        User user = userRepository.findById(userId).orElseThrow(()->new AppException(ErrorCode.USER_NOT_EXISTED));
        Movie movie = movieRepository.findById(movieId).orElseThrow(()->new AppException(ErrorCode.MOVIE_NOT_EXISTED));
            user.getFavoriteMovies().add(movie);
            userRepository.save(user);
    }

    public void removeFavoriteMovie(String userId, Long movieId){
        User user = userRepository.findById(userId).orElseThrow(()-> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.getFavoriteMovies().removeIf(m -> m.getId().equals(movieId));
            userRepository.save(user);

    }

}
