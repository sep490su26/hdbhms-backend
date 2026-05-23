package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreatePermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;

public interface CreatePermissionRequestUseCase {
    PermissionRequest execute(CreatePermissionRequestCommand command);
}
