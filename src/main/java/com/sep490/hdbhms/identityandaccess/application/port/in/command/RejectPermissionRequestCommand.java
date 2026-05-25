package com.sep490.hdbhms.identityandaccess.application.port.in.command;

public record RejectPermissionRequestCommand(Long permissionRequestId, String reason) {
}
