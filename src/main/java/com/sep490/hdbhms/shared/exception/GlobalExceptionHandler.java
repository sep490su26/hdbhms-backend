package com.sep490.hdbhms.shared.exception;

import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = DataAccessException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingDataAccessException(
            final DataAccessException e
    ) {
        log.error("A database error occurred", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<T>builder()
                        .code(ApiErrorCode.UNDEFINED.getCode())
                        .errorCode(ApiErrorCode.UNDEFINED.name())
                        .message(ApiErrorCode.UNDEFINED.getDetails())
                        .details(ApiErrorCode.UNDEFINED.getDetails())
                        .build()
        );
    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingHttpMessageNotReadableException(
            final HttpMessageNotReadableException e
    ) {
        log.warn("Invalid request data", e);
        return badRequestWithFieldError(ApiErrorCode.INVALID_REQUEST_PAYLOAD, "metadata");
    }

    @ExceptionHandler(value = MissingServletRequestPartException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingMissingServletRequestPartException(
            final MissingServletRequestPartException e
    ) {
        return badRequestWithFieldError(ApiErrorCode.INVALID_REQUEST, e.getRequestPartName());
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingMaxUploadSizeExceededException(
            final MaxUploadSizeExceededException e
    ) {
        log.warn("Uploaded file exceeds the configured size limit", e);
        return badRequestWithFieldError(ApiErrorCode.INVALID_REQUEST, "files");
    }

    @ExceptionHandler(value = MultipartException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingMultipartException(
            final MultipartException e
    ) {
        log.warn("Invalid multipart request", e);
        return badRequestWithFieldError(ApiErrorCode.INVALID_REQUEST, "metadata");
    }

    @ExceptionHandler(value = RuntimeException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingRuntimeException(
            final RuntimeException e
    ) {
        log.error("An unexpected runtime error occurred", e);
        return ResponseEntity.status(ApiErrorCode.UNDEFINED.getStatusCode()).body(
                ApiResponse.<T>builder()
                        .code(ApiErrorCode.UNDEFINED.getCode())
                        .errorCode(ApiErrorCode.UNDEFINED.name())
                        .message(ApiErrorCode.UNDEFINED.getDetails())
                        .details(ApiErrorCode.UNDEFINED.getDetails())
                        .build()
        );
    }

    @ExceptionHandler(value = AppException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingAppException(
            final AppException e
    ) {
        ApiErrorCode apiErrorCode = e.getApiErrorCode();
        String message = e.getMessage() == null ? apiErrorCode.getDetails() : e.getMessage();
        @SuppressWarnings("unchecked")
        T responseData = (T) e.getResponseData();
        return ResponseEntity.status(apiErrorCode.getStatusCode()).body(
                ApiResponse.<T>builder()
                        .code(apiErrorCode.getCode())
                        .errorCode(apiErrorCode.name())
                        .message(message)
                        .details(message)
                        .data(responseData)
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
                        .errorCode(apiErrorCode.name())
                        .message(apiErrorCode.getDetails())
                        .details(apiErrorCode.getDetails())
                        .build()
        );
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingMethodArgumentNotValidException(
            final MethodArgumentNotValidException e
    ) {
        String firstMessage = e.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse("Dữ liệu không hợp lệ.");

        try {
            ApiErrorCode apiErrorCode = ApiErrorCode.valueOf(firstMessage);
            return ResponseEntity.badRequest().body(
                    ApiResponse.<T>builder()
                            .code(apiErrorCode.getCode())
                            .errorCode(apiErrorCode.name())
                            .message(apiErrorCode.getDetails())
                            .details(apiErrorCode.getDetails())
                            .build()
            );
        } catch (IllegalArgumentException ignored) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            e.getBindingResult().getFieldErrors().forEach(error ->
                    fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
            );
            e.getBindingResult().getGlobalErrors().forEach(error ->
                    fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage())
            );
            @SuppressWarnings("unchecked")
            T validationData = (T) Map.of("fieldErrors", fieldErrors);
            String localizedMessage = ApiErrorCode.INVALID_REQUEST.getDetails();

            return ResponseEntity.badRequest().body(
                    ApiResponse.<T>builder()
                            .code(ApiErrorCode.INVALID_REQUEST.getCode())
                            .errorCode(ApiErrorCode.INVALID_REQUEST.name())
                            .message(localizedMessage)
                            .details(localizedMessage)
                            .data(validationData)
                            .build()
            );
        }
    }

    private <T> ResponseEntity<ApiResponse<T>> badRequestWithFieldError(
            ApiErrorCode apiErrorCode,
            String field
    ) {
        @SuppressWarnings("unchecked")
        T validationData = (T) Map.of("fieldErrors", Map.of(field, apiErrorCode.getDetails()));

        return ResponseEntity.badRequest().body(
                ApiResponse.<T>builder()
                        .code(apiErrorCode.getCode())
                        .errorCode(apiErrorCode.name())
                        .message(apiErrorCode.getDetails())
                        .details(apiErrorCode.getDetails())
                        .data(validationData)
                        .build()
        );
    }

}
