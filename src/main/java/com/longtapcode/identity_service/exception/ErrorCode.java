package com.longtapcode.identity_service.exception;

import org.springframework.http.HttpStatus;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1003, "User not existed", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME(1004, "username must be at least 3 character", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1005, "password must be at least 8 character", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1006, "unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    ROLE_NOT_EXISTED(1008, "Role not existed", HttpStatus.BAD_REQUEST),
    IS_NOT_ACCESS_TOKEN(1009, "This is not access token! ", HttpStatus.BAD_REQUEST),
    IS_NOT_REFRESH_TOKEN(1010, "This is not refresh token! ", HttpStatus.BAD_REQUEST),
    MOVIE_NOT_EXISTED(1011,"Movie is not existed",HttpStatus.BAD_REQUEST),
    MOVIE_EXISTED(1012,"Movie already exists",HttpStatus.BAD_REQUEST),
    SHOW_NOT_EXISTED(1013,"Show is not existed",HttpStatus.BAD_REQUEST),
    SHOW_EXISTED(1014,"Show already exists",HttpStatus.BAD_REQUEST),
    SEAT_NOT_EXISTED(1015, "Seat is not existed", HttpStatus.BAD_REQUEST),
    INCORRECT_PASSWORD(1016,"The password is incorrect. Please try again!",HttpStatus.BAD_REQUEST),
    PAYMENT_FAILED(1017, "Payment failed or cancelled",HttpStatus.BAD_REQUEST),
    SEAT_NOT_AVAILABLE(1018, "Seat is no longer available",HttpStatus.BAD_REQUEST),
    INVALID_SIGNATURE(1019, "Invalid payment signature",HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND(1020, "Payment metadata not found",HttpStatus.BAD_REQUEST);
    final int code;
    final String message;
    final HttpStatus statusCode;
}
