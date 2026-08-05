package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.LeaseContractLiquidationCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.UpdateLeaseLiquidationDraftUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateLeaseLiquidationDraftService implements UpdateLeaseLiquidationDraftUseCase {
    LeaseContractManagementService leaseContractManagementService;

    @Override
    public LeaseContractManagementResponse execute(LeaseContractLiquidationCommand command) {
        return leaseContractManagementService.updateLiquidationDraft(
                command.leaseContractId(),
                command.liquidationDate(),
                command.reason(),
                command.charges() == null
                        ? null
                        : command.charges().stream()
                        .map(charge -> new LeaseContractManagementService.LiquidationChargeInput(
                                charge.lineType(),
                                charge.description(),
                                charge.quantity(),
                                charge.unitPrice(),
                                charge.previousValue(),
                                charge.currentValue(),
                                charge.photoFileId()
                        ))
                        .toList()
        );
    }
}
