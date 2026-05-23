package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PermissionTargetType;

public record CreatePermissionRequestCommand(Long requesterUserId, PermissionTargetType targetType, Long targetId) {
}
