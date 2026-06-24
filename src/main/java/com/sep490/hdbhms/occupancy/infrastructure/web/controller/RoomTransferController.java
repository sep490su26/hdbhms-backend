package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateTransferRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ExecuteTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.HolderReplacementRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomTransferResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomTransferWebMapper;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/occupant-transfer-requests")
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

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approveTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApproveTransferCommand command = new ApproveTransferCommand(
                id,
                principal.getId()
        );
        roomTransferUseCase.approveTransfer(command);
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
    public ApiResponse<Void> confirmTransferContract(@PathVariable Long id) {
        roomTransferUseCase.confirmTransferContract(id);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/contract/sign")
    public ApiResponse<Void> signTransferContract(@PathVariable Long id) {
        roomTransferUseCase.signTransferContract(id);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/contract/reject")
    public ApiResponse<Void> rejectTransferContract(@PathVariable Long id) {
        roomTransferUseCase.rejectTransferContract(id);
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
                toCommandPayload(request == null ? null : request.transferInHandover())
        );
        roomTransferUseCase.executeTransfer(command);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Void> completeTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        CompleteTransferCommand command = new CompleteTransferCommand(
                id,
                principal.getId()
        );
        roomTransferUseCase.completeTransfer(command);
        return ApiResponse.<Void>builder().build();
    }

    // ── GET Endpoints ──────────────────────────────────────────────────────

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
                        .toList()
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
