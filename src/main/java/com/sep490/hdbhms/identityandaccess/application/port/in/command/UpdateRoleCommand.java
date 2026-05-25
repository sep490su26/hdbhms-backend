package com.sep490.hdbhms.identityandaccess.application.port.in.command;

import java.util.List;

public record UpdateRoleCommand(String roleName,
                                String newDescription,
                                List<String> newPermissions) {
}
