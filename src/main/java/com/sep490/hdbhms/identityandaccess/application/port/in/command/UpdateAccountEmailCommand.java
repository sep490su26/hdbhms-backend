package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record UpdateAccountEmailCommand(Long id, String newEmail, String currentPassword,
                                        HttpServletRequest request, HttpServletResponse response) {
}
