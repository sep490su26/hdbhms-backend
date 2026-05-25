package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreateUserCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.User;

public interface CreateUserUseCase {
    User execute(CreateUserCommand command);
}

