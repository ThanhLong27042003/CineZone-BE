package com.longtapcode.identity_service.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.longtapcode.identity_service.dto.request.ChangePassWordRequest;
import com.longtapcode.identity_service.dto.request.UpdateUserRequest;
import com.longtapcode.identity_service.dto.response.UserResponse;
import com.longtapcode.identity_service.mapper.UserMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.longtapcode.identity_service.dto.request.AuthenticationRequest;
import com.longtapcode.identity_service.dto.response.AuthenticationResponse;
import com.longtapcode.identity_service.dto.response.RefreshTokenResponse;
import com.longtapcode.identity_service.entity.RefreshToken;
import com.longtapcode.identity_service.entity.Role;
import com.longtapcode.identity_service.entity.User;
import com.longtapcode.identity_service.exception.AppException;
import com.longtapcode.identity_service.exception.ErrorCode;
import com.longtapcode.identity_service.repository.RefreshTokenRepository;
import com.longtapcode.identity_service.repository.UserRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    UserMapper userMapper;
    RefreshTokenRepository refreshTokenRepository;

    @NonFinal
    @Value("${jwt.signerKey}")
    String signerKey;

    @NonFinal
    @Value("${jwt.validationDuration}")
    long validationDuration;

    @NonFinal
    @Value("${jwt.refreshableDuration}")
    long refreshableDuration;

    public AuthenticationResponse logIn(HttpServletResponse res,AuthenticationRequest request) {
        User user = userRepository
                .findByUserName(request.getUserName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        logOutToken(request);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassWord(), user.getPassword());
        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);
        String accessToken = generateToken(user, false);
        String refreshToken = generateToken(user, true);
        setRefreshTokenCookie(res,refreshToken);
        return AuthenticationResponse.builder()
                .token(accessToken)
                .authenticated(true)
                .build();
    }

    private void logOutToken(AuthenticationRequest request) {
        User user = userRepository
                .findByUserName(request.getUserName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserId(user.getId());
        refreshToken.ifPresent(token -> refreshTokenRepository.deleteById(token.getId()));
    }

    private String generateToken(User user, boolean isRefresh) {
        String tokenId = UUID.randomUUID().toString();
        if (isRefresh) {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setId(tokenId);
            refreshToken.setUser(user);
            refreshTokenRepository.save(refreshToken);
        }
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUserName())
                .issuer("longtapcode.com")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now()
                        .plus(isRefresh ? refreshableDuration : validationDuration, ChronoUnit.SECONDS)
                        .toEpochMilli()))
                .claim("scope", isRefresh ? "" : buildScope(user.getRoles()))
                .jwtID(tokenId)
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(jwsHeader, payload);
        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    private String buildScope(Set<Role> roles) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        roles.forEach(role -> stringJoiner.add(role.getName()));
        return stringJoiner.toString();
    }

    public boolean verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier jwsVerifier = new MACVerifier(signerKey.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        boolean authenticated = signedJWT.verify(jwsVerifier);
        var scope = signedJWT.getJWTClaimsSet().getClaim("scope");
        if (isRefresh && !Objects.equals(scope, "")) {
            throw new AppException(ErrorCode.IS_NOT_REFRESH_TOKEN);
        } else if (!isRefresh && Objects.equals(scope, "")) {
            throw new AppException(ErrorCode.IS_NOT_ACCESS_TOKEN);
        }
        return authenticated && expiryTime.after(new Date());
    }

    public RefreshTokenResponse refreshToken(String refreshToken) throws ParseException, JOSEException {
        if(refreshToken == null|| refreshToken.isBlank()){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        SignedJWT signedJWT = SignedJWT.parse(refreshToken);
        String username = signedJWT.getJWTClaimsSet().getSubject();
        var user =
                userRepository.findByUserName(username).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (verifyToken(refreshToken, true)) {
            RefreshTokenResponse refreshTokenResponse = new RefreshTokenResponse();
            var accessToken = generateToken(user, false);
            refreshTokenResponse.setAccessToken(accessToken);
            return refreshTokenResponse;
        } else {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    public void setRefreshTokenCookie(HttpServletResponse res,String refreshToken){
        ResponseCookie cookie = ResponseCookie.from("refreshToken",refreshToken)
                .httpOnly(true)
                .path("/")
                .maxAge(300)
                .sameSite("Lax")
                .secure(false)
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .secure(false)
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    public void logOut (HttpServletResponse res,AuthenticationRequest request){
        var context = SecurityContextHolder.getContext();
        String userName = context.getAuthentication().getName();
        if(userName.equals(request.getUserName())) {
            logOutToken(request);
            clearRefreshTokenCookie(res);
        }
    }


    public String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public UserResponse getMyInfo(){
        var context = SecurityContextHolder.getContext();
        String userName = context.getAuthentication().getName();
        User user = userRepository.findByUserName(userName).orElseThrow(()->new AppException(ErrorCode.USER_NOT_EXISTED));
        return userMapper.toUserResponse(user);
    }

    public void updateMyInfo(UpdateUserRequest request){
        var context = SecurityContextHolder.getContext();
        String userName = context.getAuthentication().getName();
        if(userName.equals(request.getUserName())){
            User user = userRepository.findByUserName(userName).orElseThrow(()->new AppException(ErrorCode.USER_NOT_EXISTED));
            userMapper.updateUser(user,request);
            userRepository.save(user);
        }

    }

    public void changePassword(ChangePassWordRequest request ){
        var context = SecurityContextHolder.getContext();
        String userName = context.getAuthentication().getName();
        if(userName.equals(request.getUserName())){
            User user = userRepository.findByUserName(userName).orElseThrow(()->new AppException(ErrorCode.USER_NOT_EXISTED));
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
            boolean isTruePass = passwordEncoder.matches(request.getCurrentPassword(),user.getPassword());
            if(isTruePass){
                String newPass = passwordEncoder.encode(request.getNewPassword());
                user.setPassword(newPass);
                userRepository.save(user);
            }else{
                throw new AppException(ErrorCode.INCORRECT_PASSWORD);
            }
        }
    }
}
