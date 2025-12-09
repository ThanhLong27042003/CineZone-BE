package com.longtapcode.identity_service.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.CreationUserRequest;
import com.longtapcode.identity_service.dto.request.UpdateUserRequest;
import com.longtapcode.identity_service.dto.response.UserResponse;
import com.longtapcode.identity_service.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PutMapping("/updateUser/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> updateUser(
            @PathVariable("id") String id, @RequestBody @Valid UpdateUserRequest request) {
        userService.updateUserService(id, request);
        return ApiResponse.<String>builder().result("Update user successful!").build();
    }


    @PutMapping("addFavoriteMovie/{userId}/{movieId}")
    public ApiResponse<String> addFavoriteMovie(@PathVariable("userId") String userId, @PathVariable("movieId") Long movieId){
        userService.addFavoriteMovie(userId,movieId);
        return ApiResponse.<String>builder()
                .result("Add your favorite movie list successful!")
                .build();
    }
    @DeleteMapping("removeFavoriteMovie/{userId}/{movieId}")
    public ApiResponse<String> removeFavoriteMovie(@PathVariable("userId") String userId, @PathVariable("movieId") Long movieId){
        userService.removeFavoriteMovie(userId,movieId);
        return ApiResponse.<String>builder()
                .result("Remove your favorite movie list successful!")
                .build();
    }


}
