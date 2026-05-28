package com.sep490.hdbhms.identityandaccess.application.port.in.command;

public record UpdateUserFirstPasswordCommand(
        Long userId,
        String newPassword
) {
}
