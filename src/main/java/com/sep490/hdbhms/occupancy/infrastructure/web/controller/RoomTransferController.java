package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateTransferRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ExecuteTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.HolderReplacementRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.RoomTransferWebMapper;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/occupant-transfer-requests")
@RequiredArgsConstructor
public class RoomTransferController {

    private final RoomTransferUseCase roomTransferUseCase;
    private final RoomTransferWebMapper mapper;

    @PostMapping
    public ResponseEntity<Long> requestTransfer(
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
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @PostMapping("/{id}/holder-replacement")
    public ResponseEntity<Void> nominateHolder(
            @PathVariable Long id,
            @Valid @RequestBody HolderReplacementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        NominateHolderCommand command = new NominateHolderCommand(
                id,
                principal.getId(),
                request.nominatedHolderProfileId()
        );
        roomTransferUseCase.nominateHolder(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/accept-holder-nomination")
    public ResponseEntity<Void> acceptHolderNomination(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        AcceptHolderNominationCommand command = new AcceptHolderNominationCommand(
                id,
                principal.getId()
        );
        roomTransferUseCase.acceptHolderNomination(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approveTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ApproveTransferCommand command = new ApproveTransferCommand(
                id,
                principal.getId()
        );
        roomTransferUseCase.approveTransfer(command);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/target-holder/approve")
    public ResponseEntity<Void> approveTargetHolderTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomTransferUseCase.approveTargetHolderTransfer(id, principal.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/target-holder/reject")
    public ResponseEntity<Void> rejectTargetHolderTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomTransferUseCase.rejectTargetHolderTransfer(id, principal.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/contract/confirm")
    public ResponseEntity<Void> confirmTransferContract(@PathVariable Long id) {
        roomTransferUseCase.confirmTransferContract(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/contract/sign")
    public ResponseEntity<Void> signTransferContract(@PathVariable Long id) {
        roomTransferUseCase.signTransferContract(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/contract/reject")
    public ResponseEntity<Void> rejectTransferContract(@PathVariable Long id) {
        roomTransferUseCase.rejectTransferContract(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelTransferRequest(@PathVariable Long id) {
        roomTransferUseCase.cancelTransferRequest(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Void> executeTransfer(
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
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> completeTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        CompleteTransferCommand command = new CompleteTransferCommand(
                id,
                principal.getId()
        );
        roomTransferUseCase.completeTransfer(command);
        return ResponseEntity.ok().build();
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
