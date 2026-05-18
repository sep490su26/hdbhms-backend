package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import com.sep490.hdbhms.identityandaccess.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record VerifyUpdateEmailCommand(User user, String otp, HttpServletRequest request,
                                       HttpServletResponse response) {
}
