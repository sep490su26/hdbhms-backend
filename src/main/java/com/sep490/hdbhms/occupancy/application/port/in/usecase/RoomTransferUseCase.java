package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;

public interface RoomTransferUseCase {
    Long createTransferRequest(CreateTransferRequestCommand command);
    void nominateHolder(NominateHolderCommand command);
    void acceptHolderNomination(AcceptHolderNominationCommand command);
    void approveTransfer(ApproveTransferCommand command);
    void executeTransfer(ExecuteTransferCommand command);
    void completeTransfer(CompleteTransferCommand command);
}
