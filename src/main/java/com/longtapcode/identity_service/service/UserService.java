package com.longtapcode.identity_service.service;

import java.util.HashSet;
import java.util.Set;

import com.longtapcode.identity_service.dto.request.admin.AdminUpdateUserRequest;
import com.longtapcode.identity_service.entity.Movie;
import com.longtapcode.identity_service.repository.MovieRepository;
import com.longtapcode.identity_service.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@Service
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    private final MovieRepository movieRepository;
    private final RoleRepository roleRepository;

    public void updateUserService(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userMapper.updateUser(user, request);
        userRepository.save(user);
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

    // For admin
    public Page<UserResponse> getAllUsersForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page,size);
        Page<User> users = userRepository.findAll(pageable);
        return users.map(userMapper::toUserResponse);
    }

    public UserResponse createUser(CreationUserRequest request) {
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
        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUser(String userId, AdminUpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.adminUpdateUser(user, request);

        if (request.getRoles() != null) {
            var roles = roleRepository.findAllById(request.getRoles());
            user.setRoles(new HashSet<>(roles));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    public UserResponse getUserById(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    public String lockUser(String id) {
        User user = userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.toggleLock();
        userRepository.save(user);
        if(user.isLock()){
            return "This account is locked";
        }else{
            return "This account is unlocked";
        }
    }



}
