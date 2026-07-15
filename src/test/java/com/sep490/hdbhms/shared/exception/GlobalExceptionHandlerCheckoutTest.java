package com.sep490.hdbhms.shared.exception;

import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerCheckoutTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidCheckoutMetadataReturnsFieldErrorsInsteadOfRuntime500() {
        ResponseEntity<ApiResponse<Object>> response = handler.handlingHttpMessageNotReadableException(
                new HttpMessageNotReadableException("Invalid JSON")
        );

        assertEquals(400, response.getStatusCode().value());
        assertFieldError(response.getBody(), "metadata");
    }

    @Test
    void missingCheckoutMultipartPartReturnsFieldErrors() {
        ResponseEntity<ApiResponse<Object>> response = handler.handlingMissingServletRequestPartException(
                new MissingServletRequestPartException("idFrontFile")
        );

        assertEquals(400, response.getStatusCode().value());
        assertFieldError(response.getBody(), "idFrontFile");
    }

    @Test
    void oversizedCheckoutUploadReturnsFieldErrors() {
        ResponseEntity<ApiResponse<Object>> response = handler.handlingMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(30L * 1024 * 1024)
        );

        assertEquals(400, response.getStatusCode().value());
        assertFieldError(response.getBody(), "files");
    }

    @SuppressWarnings("unchecked")
    private static void assertFieldError(ApiResponse<Object> body, String field) {
        Map<String, Object> data = (Map<String, Object>) body.getData();
        Map<String, String> fieldErrors = (Map<String, String>) data.get("fieldErrors");

        assertTrue(fieldErrors.containsKey(field));
    }
}
