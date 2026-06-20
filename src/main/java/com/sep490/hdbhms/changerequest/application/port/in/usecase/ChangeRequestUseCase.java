package com.sep490.hdbhms.changerequest.application.port.in.usecase;

import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;

public interface ChangeRequestUseCase {
    void approveRequest(ApproveRequestCommand command);
    void rejectRequest(RejectRequestCommand command);
}
