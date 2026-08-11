package com.sep490.hdbhms.shared.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final ApiErrorCode apiErrorCode;

    public AppException(final ApiErrorCode apiErrorCode) {
        super(apiErrorCode.getDetails());
        this.apiErrorCode = apiErrorCode;
    }

    public AppException(final ApiErrorCode apiErrorCode, final Object... messageArguments) {
        super(formatMessage(apiErrorCode, messageArguments));
        this.apiErrorCode = apiErrorCode;
    }

    public AppException(final ApiErrorCode apiErrorCode, final Throwable cause) {
        super(apiErrorCode.getDetails(), cause);
        this.apiErrorCode = apiErrorCode;
    }

    public Object getResponseData() {
        return null;
    }

    private static String formatMessage(ApiErrorCode apiErrorCode, Object[] messageArguments) {
        return messageArguments == null || messageArguments.length == 0
                ? apiErrorCode.getDetails()
                : apiErrorCode.getDetails().formatted(messageArguments);
    }
}
