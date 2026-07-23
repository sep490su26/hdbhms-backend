package com.sep490.hdbhms.changerequest.application.port.in.usecase;

import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;

public interface ChangeRequestUseCase {
    void approveRequest(ApproveRequestCommand command);
    void rejectRequest(RejectRequestCommand command);
    ChangeRequest confirmLiquidationDepositReceipt(Long requestId, Long tenantId);
    ChangeRequest disputeLiquidationDepositRefund(Long requestId, Long tenantId, String reason);
}
