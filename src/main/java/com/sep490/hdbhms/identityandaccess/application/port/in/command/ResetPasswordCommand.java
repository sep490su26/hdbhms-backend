package com.sep490.hdbhms.identityandaccess.application.port.in.command;

public record ResetPasswordCommand(String token, String newPassword) {
}
