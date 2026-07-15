package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitTransferInspectionCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ContractInspectionUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.TransferInspectionRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/contracts")
@RequiredArgsConstructor
public class ContractInspectionController {

    private final ContractInspectionUseCase contractInspectionUseCase;

    @PostMapping("/{id}/transfer-inspections")
    public ResponseEntity<Void> submitTransferInspection(
            @PathVariable Long id,
            @Valid @RequestBody TransferInspectionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
            
        SubmitTransferInspectionCommand command = new SubmitTransferInspectionCommand(
                id,
                request.transferRequestId(),
                principal.getId(),
                request.assetInspections() != null ? request.assetInspections().stream()
                        .map(item -> new SubmitTransferInspectionCommand.AssetInspectionResult(
                                item.assetId(),
                                item.condition(),
                                item.compensationAmount(),
                                item.description()
                        )).collect(Collectors.toList()) : null,
                request.notes()
        );
        
        contractInspectionUseCase.submitTransferInspection(command);
        return ResponseEntity.ok().build();
    }
}
