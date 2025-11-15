package com.longtapcode.identity_service.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.context.SecurityContextHolder;
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
    public UserResponse getMyInfo(){
        var context = SecurityContextHolder.getContext();
        String userName = context.getAuthentication().getName();
        User user = userRepository.findByUserName(userName).orElseThrow(()->new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }
}
