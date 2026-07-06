package com.sep490.hdbhms.changerequest.application.port.in.command;

public record ApproveRequestCommand(Long requestId, Long managerId, String durationCode) {
    public ApproveRequestCommand(Long requestId, Long managerId) {
        this(requestId, managerId, null);
    }
}
