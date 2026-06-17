package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.AcceptHolderNominationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CompleteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreateTransferRequestCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ExecuteTransferCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.NominateHolderCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.RoomTransferUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateTransferRequestRequest;
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

    @PostMapping("/{id}/execute")
    public ResponseEntity<Void> executeTransfer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ExecuteTransferCommand command = new ExecuteTransferCommand(
                id,
                principal.getId()
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
}
