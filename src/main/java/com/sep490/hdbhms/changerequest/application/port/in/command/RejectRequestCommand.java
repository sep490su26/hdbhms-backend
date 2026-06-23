package com.sep490.hdbhms.changerequest.application.port.in.command;

public record RejectRequestCommand(Long requestId, Long managerId, String resolutionNote) {}
