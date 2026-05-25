package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import com.sep490.hdbhms.identityandaccess.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public record UpdateAccountUsernameCommand(User user, String newUsername, String currentPassword,
                                           HttpServletRequest request, HttpServletResponse response) {
}
