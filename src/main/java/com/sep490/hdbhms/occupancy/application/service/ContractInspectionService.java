package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitTransferInspectionCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.ContractInspectionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractInspectionService implements ContractInspectionUseCase {

    @Override
    @Transactional
    public void submitTransferInspection(SubmitTransferInspectionCommand command) {
        log.info("Submitting transfer inspection for contract {} and transfer request {}", 
                command.contractId(), command.transferRequestId());
        // TODO: Implement actual inspection logic, asset condition updates, and compensation charges
    }
}
