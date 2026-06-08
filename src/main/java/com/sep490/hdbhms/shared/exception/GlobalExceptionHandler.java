package com.sep490.hdbhms.shared.exception;

import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(value = DataAccessException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingDataAccessException(
            final DataAccessException e
    ) {
        log.error("Database error occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<T>builder()
                        .code(500)
                        .message("Lỗi hệ thống khi truy vấn dữ liệu. Vui lòng thử lại.")
                        .build()
        );
    }

    @ExceptionHandler(value = RuntimeException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingRuntimeException(
            final RuntimeException e
    ) {
        log.error("Unexpected runtime error", e);
        return ResponseEntity.status(ApiErrorCode.UNDEFINED.getStatusCode()).body(
                ApiResponse.<T>builder()
                        .code(ApiErrorCode.UNDEFINED.getCode())
                        .message(ApiErrorCode.UNDEFINED.getMessage())
                        .details("Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.")
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
        String firstMessage = Objects
                .requireNonNull(e.getBindingResult().getFieldError())
                .getDefaultMessage();

        try {
            ApiErrorCode apiErrorCode = ApiErrorCode.valueOf(firstMessage);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<T>builder()
                            .code(apiErrorCode.getCode())
                            .message(apiErrorCode.getMessage())
                            .details(apiErrorCode.getDetails())
                            .build()
            );
        } catch (IllegalArgumentException ignored) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            e.getBindingResult().getFieldErrors().forEach(error ->
                    fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
            );

            @SuppressWarnings("unchecked")
            T validationData = (T) Map.of("fieldErrors", fieldErrors);

            return ResponseEntity.badRequest().body(
                    ApiResponse.<T>builder()
                            .code(400)
                            .message("Dữ liệu không hợp lệ.")
                            .details(firstMessage)
                            .data(validationData)
                            .build()
            );
        }
    }
}
