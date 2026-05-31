package com.sep490.hdbhms.shared.exception;

import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = ResponseStatusException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingResponseStatusException(
            final ResponseStatusException e
    ) {
        return ResponseEntity.status(e.getStatusCode()).body(
                ApiResponse.<T>builder()
                        .code(e.getStatusCode().value())
                        .message(e.getReason())
                        .details(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = RuntimeException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingRuntimeException(
            final RuntimeException e
    ) {
        return ResponseEntity.status(ApiErrorCode.UNDEFINED.getStatusCode()).body(
                ApiResponse.<T>builder()
                        .code(ApiErrorCode.UNDEFINED.getCode())
                        .message(ApiErrorCode.UNDEFINED.getMessage())
                        .details(e.getMessage())
                        .build()
        );
    }

    @ExceptionHandler(value = AppException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingAppException(
            final AppException e
    ) {
        ApiErrorCode apiErrorCode = e.getApiErrorCode();
        return ResponseEntity.status(apiErrorCode.getStatusCode()).body(
                ApiResponse.<T>builder()
                        .code(apiErrorCode.getCode())
                        .message(apiErrorCode.getMessage())
                        .details(apiErrorCode.getDetails())
                        .build()
        );
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingAccessDeniedException(
            final AccessDeniedException e
    ) {
        ApiErrorCode apiErrorCode = ApiErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(apiErrorCode.getStatusCode()).body(
                ApiResponse.<T>builder()
                        .code(apiErrorCode.getCode())
                        .message(apiErrorCode.getMessage())
                        .details(apiErrorCode.getDetails())
                        .build()
        );
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingMethodArgumentNotValidException(
            final MethodArgumentNotValidException e
    ) {
        ApiErrorCode apiErrorCode = ApiErrorCode.valueOf(
                Objects
                        .requireNonNull(e.getBindingResult().getFieldError())
                        .getDefaultMessage()
        );
        return ResponseEntity.badRequest().body(
                ApiResponse.<T>builder()
                        .code(apiErrorCode.getCode())
                        .message(apiErrorCode.getMessage())
                        .details(apiErrorCode.getDetails())
                        .build()
        );
    }
}
