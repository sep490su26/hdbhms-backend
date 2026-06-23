package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestDecisionHandler;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomTransferChangeRequestDecisionHandler implements ChangeRequestDecisionHandler {
    private final RoomTransferUseCase roomTransferUseCase;

    @Override
    public boolean supports(RequestType requestType) {
        return requestType == RequestType.ROOM_TRANSFER;
    }

    @Override
    public void onApproved(ChangeRequest request, Long managerId) {
        roomTransferUseCase.approveTransfer(new ApproveTransferCommand(request.getTargetId(), managerId));
    }

    @Override
    public void onRejected(ChangeRequest request, Long managerId, String resolutionNote) {
        roomTransferUseCase.rejectTransferRequest(request.getTargetId(), managerId, resolutionNote);
    }
}
