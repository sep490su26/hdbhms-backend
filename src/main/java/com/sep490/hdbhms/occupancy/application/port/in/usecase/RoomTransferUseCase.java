package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;

import java.util.List;

public interface RoomTransferUseCase {
    Long createTransferRequest(CreateTransferRequestCommand command);
    void nominateHolder(NominateHolderCommand command);
    void acceptHolderNomination(AcceptHolderNominationCommand command);
    void approveTransfer(ApproveTransferCommand command);
    void rejectTransferRequest(Long requestId, Long managerId, String resolutionNote);
    void approveTargetHolderTransfer(Long requestId, Long holderUserId);
    void rejectTargetHolderTransfer(Long requestId, Long holderUserId);
    void confirmTransferContract(Long requestId);
    void signTransferContract(Long requestId);
    void rejectTransferContract(Long requestId);
    void cancelTransferRequest(Long requestId);
    void executeTransfer(ExecuteTransferCommand command);
    void completeTransfer(CompleteTransferCommand command);
    int expireTargetHolderApprovals();
    RoomTransferRequest getTransferRequestById(Long requestId);
    List<RoomTransferRequest> getPendingTargetHolderApprovals(Long holderUserId);
}
