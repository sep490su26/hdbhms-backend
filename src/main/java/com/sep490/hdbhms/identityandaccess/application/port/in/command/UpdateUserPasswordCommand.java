package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record UpdateUserPasswordCommand(
        Long userId,
        String currentPassword,
        String newPassword,
        HttpServletRequest request,
        HttpServletResponse response
) {
}
