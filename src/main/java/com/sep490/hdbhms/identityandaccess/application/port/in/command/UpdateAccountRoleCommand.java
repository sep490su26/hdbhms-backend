package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;

public record UpdateAccountRoleCommand(Long id, Role newRole) {
}
