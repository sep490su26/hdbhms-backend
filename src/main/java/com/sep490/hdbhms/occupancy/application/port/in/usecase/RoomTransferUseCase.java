package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmTenantTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TransferOutUtilityEstimateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RoomTransferUseCase {
    Long createTransferRequest(CreateTransferRequestCommand command);
    void nominateHolder(NominateHolderCommand command);
    void acceptHolderNomination(AcceptHolderNominationCommand command);
    void rejectHolderNomination(Long requestId, Long tenantUserId);
    void confirmTenantTransfer(ConfirmTenantTransferCommand command);
    void approveTransfer(ApproveTransferCommand command);
    void rejectTransferRequest(Long requestId, Long managerId, String resolutionNote);
    void approveTargetHolderTransfer(Long requestId, Long holderUserId);
    void rejectTargetHolderTransfer(Long requestId, Long holderUserId);
    void confirmTransferContract(Long requestId, Long tenantUserId);
    void advanceTransferAfterDifferencePayment(Long requestId, Long tenantUserId);
    void signTransferContract(Long requestId, Long tenantUserId);
    void rejectTransferContract(Long requestId, Long tenantUserId);
    void cancelTransferRequest(Long requestId);
    void executeTransfer(ExecuteTransferCommand command);
    void completeTransfer(CompleteTransferCommand command);
    TransferOutUtilityEstimateResponse estimateTransferOutUtility(ExecuteTransferCommand command);
    int expireTargetHolderApprovals();
    int expireSourceHolderNominations();
    RoomTransferRequest refreshTransferEligibilitySnapshot(Long requestId);
    RoomTransferRequest getTransferRequestById(Long requestId);
    RoomTransferRequest getTransferRequestByCode(String requestCode);
    Page<RoomTransferRequest> getTransferHistory(
            TransferRequestStatus status,
            Long floorId,
            Long roomId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );
    List<RoomTransferRequest> getPendingHolderNominations(Long holderUserId);
    List<RoomTransferRequest> getPendingTargetHolderApprovals(Long holderUserId);
}
