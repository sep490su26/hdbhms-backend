package com.sep490.hdbhms.identityandaccess.application.port.in.command;

public record UpdateAccountEmailCommand(Long id, String newEmail, String currentPassword) {
}
