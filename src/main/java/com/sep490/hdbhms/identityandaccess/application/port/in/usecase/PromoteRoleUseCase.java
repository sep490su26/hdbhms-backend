package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.PromoteRoleCommand;

public interface PromoteRoleUseCase {
    void execute(PromoteRoleCommand command);
}
