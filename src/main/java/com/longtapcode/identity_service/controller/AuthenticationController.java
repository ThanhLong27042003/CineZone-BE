package com.longtapcode.identity_service.controller;

import java.text.ParseException;

import com.longtapcode.identity_service.dto.request.*;
import com.longtapcode.identity_service.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

import com.longtapcode.identity_service.dto.response.AuthenticationResponse;
import com.longtapcode.identity_service.dto.response.RefreshTokenResponse;
import com.longtapcode.identity_service.service.AuthenticationService;
import com.nimbusds.jose.JOSEException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/log-in")
    public ApiResponse<AuthenticationResponse> logIn(HttpServletResponse res, @RequestBody @Valid AuthenticationRequest request) {
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.logIn(res,request))
                .build();
    }
    @PostMapping("/log-out")
    public ApiResponse<?> logOut(HttpServletResponse res, @RequestBody @Valid AuthenticationRequest request) {
        authenticationService.logOut(res,request);
        return ApiResponse.<String>builder()
                .result("User loged out!!")
                .build();
    }


    @PostMapping("/refreshToken")
    public ApiResponse<RefreshTokenResponse> refreshToken(HttpServletRequest req)
            throws ParseException, JOSEException {
        String refreshToken = authenticationService.extractRefreshToken(req);
        return ApiResponse.<RefreshTokenResponse>builder()
                .result(authenticationService.refreshToken(refreshToken))
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo(){
        return ApiResponse.<UserResponse>builder()
                .result(authenticationService.getMyInfo())
                .build();
    }

    @PostMapping("/update/me")
    public ApiResponse<?> updateMyInfo(@RequestBody UpdateUserRequest request){
        authenticationService.updateMyInfo(request);
        return ApiResponse.<String>builder()
                .result("Profile updated successfully! ✅")
                .build();
    }

    @PostMapping("/changePassWord/me")
    public ApiResponse<?> changePassWord(@RequestBody ChangePassWordRequest request){
        authenticationService.changePassword(request);
        return ApiResponse.<String>builder()
                .result("Change password successfully! ✅")
                .build();
    }
}
