package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.AddCoOccupantToContractCommand;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;

public interface AddCoOccupantToContractUseCase {
    LeaseContractManagementResponse execute(AddCoOccupantToContractCommand command);
}
