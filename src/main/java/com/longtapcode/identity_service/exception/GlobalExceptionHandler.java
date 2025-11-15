package com.longtapcode.identity_service.exception;

import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.longtapcode.identity_service.dto.request.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<Void>> handlingRuntimeException(Exception exception) {
        log.error("Exception: ", exception);
        ApiResponse<Void> apiResponse = new ApiResponse<>();

        apiResponse.setCode(ErrorCode.UNCATEGORIZED_EXCEPTION.getCode());
        apiResponse.setMessage(ErrorCode.UNCATEGORIZED_EXCEPTION.getMessage());

        return ResponseEntity.status(ErrorCode.UNCATEGORIZED_EXCEPTION.getStatusCode())
                .body(apiResponse);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<Void>> handingAppException(AppException exception) {
        ApiResponse<Void> apiResponse = new ApiResponse<>();
        apiResponse.setCode(exception.getErrorCode().getCode());
        apiResponse.setMessage(exception.getErrorCode().getMessage());
        return ResponseEntity.status(exception.getErrorCode().getStatusCode()).body(apiResponse);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handlingValidationException(MethodArgumentNotValidException exception) {
        ApiResponse<Void> apiResponse = new ApiResponse<>();
        String errorKey = Objects.requireNonNull(exception.getFieldError()).getDefaultMessage();
        try {
            ErrorCode errorCode = ErrorCode.valueOf(errorKey);
            apiResponse.setMessage(errorCode.getMessage());
            apiResponse.setCode(errorCode.getCode());
            return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
        } catch (IllegalArgumentException e) {
            ErrorCode errorCode = ErrorCode.INVALID_KEY;
            apiResponse.setMessage(errorCode.getMessage());
            apiResponse.setCode(errorCode.getCode());
            return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
        }
    }

    @ExceptionHandler(value = AuthorizationDeniedException.class)
    ResponseEntity<ApiResponse<Object>> handlingAuthorizationDeniedException(AuthorizationDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }
}
