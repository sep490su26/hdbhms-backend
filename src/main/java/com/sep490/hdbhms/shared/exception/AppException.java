package com.sep490.hdbhms.shared.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {
    private final ApiErrorCode apiErrorCode;

    public AppException(final ApiErrorCode apiErrorCode) {
        super(apiErrorCode.getMessage());
        this.apiErrorCode = apiErrorCode;
    }
}
