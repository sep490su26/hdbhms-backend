package com.sep490.hdbhms.shared.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum ErrorCode {
    UNDEFINED(-1, "Undefined", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_USERNAME(601, "Username must have more than 3 characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(602, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    ACCOUNT_EXISTED(603, "Account already exists", HttpStatus.CONFLICT),
    ACCOUNT_NOT_FOUND(604, "Account not found", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(701, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(702, "You do not have permission", HttpStatus.FORBIDDEN),
    SERVICE_NOT_FOUND(703, "Requested service in unavailable", HttpStatus.NOT_FOUND),
    FILE_READ_ERROR(710, "Reading file error", HttpStatus.INTERNAL_SERVER_ERROR);
    int code;
    String message;
    HttpStatusCode statusCode;
}
