package com.sep490.hdbhms.identityandaccess.application.port.in.command;


public record UpdateAccountCommand(String username, String email, String password) {
}
