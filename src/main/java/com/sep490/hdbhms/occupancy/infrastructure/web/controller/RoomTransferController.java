package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmTenantTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateTransferRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ConfirmTenantTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ExecuteTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.HolderReplacementRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomTransferResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TransferOutUtilityEstimateResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomTransferWebMapper;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/occupant-transfer-requests")
@RequiredArgsConstructor
public class RoomTransferController {

    private final RoomTransferUseCase roomTransferUseCase;
    private final RoomTransferWebMapper mapper;

    @PostMapping
    public ApiResponse<Long> requestTransfer(
            @Valid @RequestBody CreateTransferRequestRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        CreateTransferRequestCommand command = mapper.toCommand(request);
        CreateTransferRequestCommand finalCommand = new CreateTransferRequestCommand(
                principal.getId(),
                command.sourceContractId(),
                command.targetRoomId(),
                command.requestedTransferDate(),
                command.transferredTenantProfileIds(),
                command.reason()
        );

        Long id = roomTransferUseCase.createTransferRequest(finalCommand);
        return ApiResponse.<Long>builder()
                .data(id)
                .build();
    }

    @PostMapping("/{id}/holder-replacement")
    public ApiResponse<Void> nominateHolder(
            @PathVariable Long id,
            @Valid @RequestBody HolderReplacementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        NominateHolderCommand command = new NominateHolderCommand(
                id,
                principal.getId(),
                request.nominatedHolderProfileId()
        );
        roomTransferUseCase.nominateHolder(command);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/accept-holder-nomination")
    public ApiResponse<Void> acceptHolderNomination(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        AcceptHolderNominationCommand command = new AcceptHolderNominationCommand(
                id,
                principal.getId()
        );
        roomTransferUseCase.acceptHolderNomination(command);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/reject-holder-nomination")
    public ApiResponse<Void> rejectHolderNomination(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomTransferUseCase.rejectHolderNomination(id, principal.getId());
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approveTransfer(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmTenantTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApproveTransferCommand command = new ApproveTransferCommand(
                id,
                principal.getId(),
                request.settlementType()
        );
        roomTransferUseCase.approveTransfer(command);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<Void> confirmTenantTransfer(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmTenantTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ConfirmTenantTransferCommand command = new ConfirmTenantTransferCommand(
                id,
                principal.getId(),
                request.settlementType(),
                request.nominatedHolderProfileId()
        );
        roomTransferUseCase.confirmTenantTransfer(command);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/target-holder/approve")
    public ApiResponse<Void> approveTargetHolderTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomTransferUseCase.approveTargetHolderTransfer(id, principal.getId());
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/target-holder/reject")
    public ApiResponse<Void> rejectTargetHolderTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomTransferUseCase.rejectTargetHolderTransfer(id, principal.getId());
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/contract/confirm")
    public ApiResponse<Void> confirmTransferContract(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomTransferUseCase.confirmTransferContract(id, principal.getId());
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/contract/sign")
    public ApiResponse<Void> signTransferContract(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomTransferUseCase.signTransferContract(id, principal.getId());
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/contract/reject")
    public ApiResponse<Void> rejectTransferContract(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomTransferUseCase.rejectTransferContract(id, principal.getId());
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelTransferRequest(@PathVariable Long id) {
        roomTransferUseCase.cancelTransferRequest(id);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/execute")
    public ApiResponse<Void> executeTransfer(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ExecuteTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExecuteTransferCommand command = new ExecuteTransferCommand(
                id,
                principal.getId(),
                toCommandPayload(request == null ? null : request.transferOutHandover()),
                toCommandPayload(request == null ? null : request.transferInHandover()),
                request != null ? request.positiveDifferenceSettlementType() : null,
                request != null ? request.oldRoomCompensationAmount() : null,
                request != null ? request.oldRoomCompensationNote() : null
        );
        roomTransferUseCase.executeTransfer(command);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/transfer-out-utility-estimate")
    public ApiResponse<TransferOutUtilityEstimateResponse> estimateTransferOutUtility(
            @PathVariable Long id,
            @Valid @RequestBody ExecuteTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExecuteTransferCommand command = new ExecuteTransferCommand(
                id,
                principal.getId(),
                toCommandPayload(request == null ? null : request.transferOutHandover()),
                null,
                null,
                null,
                null
        );
        return ApiResponse.<TransferOutUtilityEstimateResponse>builder()
                .data(roomTransferUseCase.estimateTransferOutUtility(command))
                .build();
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Void> completeTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        CompleteTransferCommand command = new CompleteTransferCommand(
                id,
                principal.getId(),
                null,
                null
        );
        roomTransferUseCase.completeTransfer(command);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/complete-with-handover")
    public ApiResponse<Void> completeTransferWithHandover(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ExecuteTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        CompleteTransferCommand command = new CompleteTransferCommand(
                id,
                principal.getId(),
                toCommandPayload(request == null ? null : request.transferInHandover()),
                request != null ? request.positiveDifferenceSettlementType() : null
        );
        roomTransferUseCase.completeTransfer(command);
        return ApiResponse.<Void>builder().build();
    }

    // ── GET Endpoints ──────────────────────────────────────────────────────

    @GetMapping("/code/{requestCode}")
    public ApiResponse<RoomTransferResponse> getTransferRequestByCode(
            @PathVariable String requestCode) {
        RoomTransferRequest request = roomTransferUseCase.getTransferRequestByCode(requestCode);
        return ApiResponse.<RoomTransferResponse>builder()
                .data(mapper.toResponse(request))
                .build();
    }

    @GetMapping("/history")
    public ApiResponse<PageResponse<RoomTransferResponse>> getTransferHistory(
            @RequestParam(required = false, defaultValue = "EXECUTED") TransferRequestStatus status,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @PageableDefault(size = 10, sort = "executedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<RoomTransferResponse> page = roomTransferUseCase
                .getTransferHistory(status, floorId, roomId, fromDate, toDate, pageable)
                .map(mapper::toResponse);
        return ApiResponse.<PageResponse<RoomTransferResponse>>builder()
                .code(0)
                .data(PageResponse.fromPageToPageResponse(page))
                .build();
    }


    @GetMapping("/{id}")
    public ApiResponse<RoomTransferResponse> getTransferRequest(
            @PathVariable Long id) {
        RoomTransferRequest request = roomTransferUseCase.getTransferRequestById(id);
        return ApiResponse.<RoomTransferResponse>builder()
                .data(mapper.toResponse(request))
                .build();
    }

    @GetMapping("/pending-target-holder-approvals")
    public ApiResponse<List<RoomTransferResponse>> getPendingTargetHolderApprovals(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RoomTransferRequest> requests = roomTransferUseCase
                .getPendingTargetHolderApprovals(principal.getId());
        return ApiResponse.<List<RoomTransferResponse>>builder()
                .data(requests.stream()
                        .map(mapper::toResponse)
                        .toList())
                .build();
    }

    @GetMapping("/pending-holder-nominations")
    public ApiResponse<List<RoomTransferResponse>> getPendingHolderNominations(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RoomTransferRequest> requests = roomTransferUseCase
                .getPendingHolderNominations(principal.getId());
        return ApiResponse.<List<RoomTransferResponse>>builder()
                .data(requests.stream()
                        .map(mapper::toResponse)
                        .toList())
                .build();
    }

    private ExecuteTransferCommand.TransferHandoverData toCommandPayload(ExecuteTransferRequest.TransferHandoverPayload payload) {
        if (payload == null) {
            return null;
        }
        return new ExecuteTransferCommand.TransferHandoverData(
                payload.handoverDate(),
                payload.note(),
                toCommandMeter(payload.electricity()),
                toCommandMeter(payload.water()),
                payload.assets() == null
                        ? null
                        : payload.assets().stream()
                        .map(this::toCommandAsset)
                        .toList(),
                payload.incidentalChargeAmount(),
                payload.incidentalChargeNote()
        );
    }

    private ExecuteTransferCommand.MeterReadingData toCommandMeter(ExecuteTransferRequest.MeterReadingPayload payload) {
        if (payload == null) {
            return null;
        }
        return new ExecuteTransferCommand.MeterReadingData(
                payload.currentValue(),
                payload.photoFileId(),
                payload.readingDate()
        );
    }

    private ExecuteTransferCommand.AssetData toCommandAsset(ExecuteTransferRequest.AssetPayload payload) {
        return new ExecuteTransferCommand.AssetData(
                payload.id(),
                payload.assetName(),
                payload.assetCategory(),
                payload.quantity(),
                payload.currentCondition(),
                payload.description(),
                payload.fileImageId()
        );
    }
}
