package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;

public record PromoteRoleCommand(Long userId, Role toRole) {
}
