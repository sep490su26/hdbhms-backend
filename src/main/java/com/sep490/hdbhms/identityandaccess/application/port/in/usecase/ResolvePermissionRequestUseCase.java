package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.ApprovePermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.command.RejectPermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;

public interface ResolvePermissionRequestUseCase {
    PermissionRequest approve(ApprovePermissionRequestCommand command);

    PermissionRequest reject(RejectPermissionRequestCommand command);
}
