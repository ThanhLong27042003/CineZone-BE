package com.longtapcode.identity_service.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.longtapcode.identity_service.dto.request.AuthenticationRequest;
import com.longtapcode.identity_service.dto.request.ChangePassWordRequest;
import com.longtapcode.identity_service.dto.request.UpdateUserRequest;
import com.longtapcode.identity_service.dto.response.AuthenticationResponse;
import com.longtapcode.identity_service.dto.response.UserResponse;
import com.longtapcode.identity_service.entity.RefreshToken;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.mapper.UserMapper;
import com.longtapcode.identity_service.repository.RefreshTokenRepository;
import com.longtapcode.identity_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User user;
    private PasswordEncoder passwordEncoder;
    private final String SIGNER_KEY = "1234567890123456789012345678901234567890123456789012345678901234";

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(10);

        user = new User();
        user.setId("user-123");
        user.setUserName("testUser");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setLock(false);
        user.setRoles(new java.util.HashSet<>());

        ReflectionTestUtils.setField(authenticationService, "signerKey", SIGNER_KEY);
        ReflectionTestUtils.setField(authenticationService, "validationDuration", 3600L);
        ReflectionTestUtils.setField(authenticationService, "refreshableDuration", 86400L);
    }

    private void mockSecurityContext(String username) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(username);
        SecurityContextHolder.setContext(securityContext);
    }

    @Nested
    @DisplayName("logIn")
    class LogIn {

        @Test
        @DisplayName("Success - Valid credentials")
        void logIn_Success() {
            AuthenticationRequest authRequest = new AuthenticationRequest("testUser", "password123");
            when(userRepository.findByUserName("testUser")).thenReturn(Optional.of(user));
            when(refreshTokenRepository.findByUserId("user-123")).thenReturn(Optional.empty());

            AuthenticationResponse result = authenticationService.logIn(response, authRequest);

            assertTrue(result.isAuthenticated());
            assertNotNull(result.getToken());
            verify(refreshTokenRepository).save(any(RefreshToken.class));
            verify(response).addHeader(eq("Set-Cookie"), anyString());
        }

        @Test
        @DisplayName("Fail - Incorrect password")
        void logIn_Fail_IncorrectPassword() {
            AuthenticationRequest authRequest = new AuthenticationRequest("testUser", "wrongPass");
            when(userRepository.findByUserName("testUser")).thenReturn(Optional.of(user));

            AppException ex =
                    assertThrows(AppException.class, () -> authenticationService.logIn(response, authRequest));

            assertEquals(ErrorCode.INCORRECT_PASSWORD, ex.getErrorCode());
        }

        @Test
        @DisplayName("Fail - Account locked")
        void logIn_Fail_AccountLocked() {
            user.setLock(true);
            AuthenticationRequest authRequest = new AuthenticationRequest("testUser", "password123");
            when(userRepository.findByUserName("testUser")).thenReturn(Optional.of(user));

            AppException ex =
                    assertThrows(AppException.class, () -> authenticationService.logIn(response, authRequest));

            assertEquals(ErrorCode.ACCOUNT_IS_LOCKED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getMyInfo")
    class GetMyInfo {

        @Test
        @DisplayName("Success")
        void getMyInfo_Success() {
            mockSecurityContext("testUser");
            when(userRepository.findByUserName("testUser")).thenReturn(Optional.of(user));

            UserResponse userResponse = new UserResponse();
            userResponse.setUserName("testUser");
            when(userMapper.toUserResponse(user)).thenReturn(userResponse);

            UserResponse result = authenticationService.getMyInfo();

            assertEquals("testUser", result.getUserName());
        }

        @Test
        @DisplayName("Fail - User not found")
        void getMyInfo_Fail() {
            mockSecurityContext("unknown");
            when(userRepository.findByUserName("unknown")).thenReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class, () -> authenticationService.getMyInfo());

            assertEquals(ErrorCode.USER_NOT_EXISTED, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("updateMyInfo")
    class UpdateMyInfo {

        @Test
        @DisplayName("Success")
        void updateMyInfo_Success() {
            mockSecurityContext("testUser");
            UpdateUserRequest updateRequest = new UpdateUserRequest(
                    "testUser",
                    "John",
                    "Doe",
                    "0123456789",
                    "Address",
                    "email@test.com",
                    "avatar.jpg",
                    java.time.LocalDate.now());

            when(userRepository.findByUserName("testUser")).thenReturn(Optional.of(user));

            assertDoesNotThrow(() -> authenticationService.updateMyInfo(updateRequest));

            verify(userMapper).updateUser(user, updateRequest);
            verify(userRepository).save(user);
        }
    }

    @Nested
    @DisplayName("changePassword")
    class ChangePassword {

        @Test
        @DisplayName("Success")
        void changePassword_Success() {
            mockSecurityContext("testUser");
            ChangePassWordRequest req = new ChangePassWordRequest();
            req.setUserName("testUser");
            req.setCurrentPassword("password123");
            req.setNewPassword("newPassword123");

            when(userRepository.findByUserName("testUser")).thenReturn(Optional.of(user));

            assertDoesNotThrow(() -> authenticationService.changePassword(req));

            verify(userRepository).save(argThat(u -> passwordEncoder.matches("newPassword123", u.getPassword())));
        }

        @Test
        @DisplayName("Fail - Incorrect current password")
        void changePassword_Fail_IncorrectPassword() {
            mockSecurityContext("testUser");
            ChangePassWordRequest req = new ChangePassWordRequest();
            req.setUserName("testUser");
            req.setCurrentPassword("wrongPass");
            req.setNewPassword("newPassword123");

            when(userRepository.findByUserName("testUser")).thenReturn(Optional.of(user));

            AppException ex = assertThrows(AppException.class, () -> authenticationService.changePassword(req));

            assertEquals(ErrorCode.INCORRECT_PASSWORD, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("extractRefreshToken")
    class ExtractRefreshToken {

        @Test
        @DisplayName("Success - found in cookies")
        void extractRefreshToken_Success() {
            Cookie[] cookies = {new Cookie("refreshToken", "sample-token")};
            when(request.getCookies()).thenReturn(cookies);

            String result = authenticationService.extractRefreshToken(request);

            assertEquals("sample-token", result);
        }

        @Test
        @DisplayName("Returns null when no cookies")
        void extractRefreshToken_NoCookies() {
            when(request.getCookies()).thenReturn(null);

            String result = authenticationService.extractRefreshToken(request);

            assertNull(result);
        }
    }
}
