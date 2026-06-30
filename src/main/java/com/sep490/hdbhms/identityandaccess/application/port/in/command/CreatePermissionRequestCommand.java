package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionTargetType;

public record CreatePermissionRequestCommand(Long requesterUserId, PermissionTargetType targetType, Long targetId) {
}
