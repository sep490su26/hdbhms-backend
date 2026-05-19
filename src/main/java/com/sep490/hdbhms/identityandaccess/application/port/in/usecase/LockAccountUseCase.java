package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.LockAccountCommand;

public interface LockAccountUseCase {
    void execute(LockAccountCommand command);
}
