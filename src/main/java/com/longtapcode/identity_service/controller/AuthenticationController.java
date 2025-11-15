package com.longtapcode.identity_service.controller;

import java.text.ParseException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.longtapcode.identity_service.dto.request.ApiResponse;
import com.longtapcode.identity_service.dto.request.AuthenticationRequest;
import com.longtapcode.identity_service.dto.request.VerifyRequest;
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


    @PostMapping("/refreshToken")
    public ApiResponse<RefreshTokenResponse> refreshToken(HttpServletRequest req)
            throws ParseException, JOSEException {
        String refreshToken = authenticationService.extractRefreshToken(req);
        return ApiResponse.<RefreshTokenResponse>builder()
                .result(authenticationService.refreshToken(refreshToken))
                .build();
    }
}
