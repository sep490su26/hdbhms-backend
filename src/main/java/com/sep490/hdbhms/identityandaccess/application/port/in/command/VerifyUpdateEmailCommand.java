package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record VerifyUpdateEmailCommand(
        Long userId,
        String otp,
        HttpServletRequest request,
        HttpServletResponse response
) {
}
