package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.command.StartLeaseLiquidationProcessingCommand;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.StartLeaseLiquidationProcessingUseCase;
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
public class StartLeaseLiquidationProcessingService implements StartLeaseLiquidationProcessingUseCase {
    LeaseContractManagementService leaseContractManagementService;

    @Override
    public LeaseContractManagementResponse execute(StartLeaseLiquidationProcessingCommand command) {
        return leaseContractManagementService.startLiquidationProcessing(
                command.leaseContractId(),
                command.liquidationDate(),
                command.reason(),
                command.liquidationMode(),
                command.leavingProfileIds(),
                command.stayingProfileIds(),
                command.replacementPrimaryTenantProfileId()
        );
    }
}
