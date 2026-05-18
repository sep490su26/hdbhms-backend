package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import com.sep490.hdbhms.identityandaccess.domain.model.User;

public record RequestVerifyEmailCommand(User user) {
}
