package com.sep490.hdbhms.shared.exception;

import com.sep490.hdbhms.shared.dto.response.ApiResponse;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

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

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingHttpMessageNotReadableException(
            final HttpMessageNotReadableException e
    ) {
        log.warn("Invalid request payload", e);
        return badRequestWithFieldError(
                "metadata",
                "Dữ liệu đặt cọc không đúng định dạng. Vui lòng kiểm tra lại thông tin."
        );
    }

    @ExceptionHandler(value = MissingServletRequestPartException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingMissingServletRequestPartException(
            final MissingServletRequestPartException e
    ) {
        return badRequestWithFieldError(
                e.getRequestPartName(),
                "Thiếu thông tin hoặc tệp bắt buộc: " + e.getRequestPartName()
        );
    }

    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingMaxUploadSizeExceededException(
            final MaxUploadSizeExceededException e
    ) {
        log.warn("Multipart upload exceeds configured size limit", e);
        return badRequestWithFieldError(
                "files",
                "Tệp tải lên quá lớn. Mỗi ảnh tối đa 10MB và tổng dung lượng tối đa 30MB."
        );
    }

    @ExceptionHandler(value = MultipartException.class)
    <T> ResponseEntity<ApiResponse<T>> handlingMultipartException(
            final MultipartException e
    ) {
        log.warn("Invalid multipart request", e);
        return badRequestWithFieldError(
                "metadata",
                "Không thể đọc dữ liệu đặt cọc. Vui lòng kiểm tra lại tệp tải lên và thử lại."
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
                            .message(apiErrorCode.getMessage())
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

    private <T> ResponseEntity<ApiResponse<T>> badRequestWithFieldError(String field, String message) {
        @SuppressWarnings("unchecked")
        T validationData = (T) Map.of("fieldErrors", Map.of(field, message));

        return ResponseEntity.badRequest().body(
                ApiResponse.<T>builder()
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message("Dữ liệu không hợp lệ.")
                        .details(message)
                        .data(validationData)
                        .build()
        );
    }
}
