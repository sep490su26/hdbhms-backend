package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.RenewLeaseContractCommand;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractRenewalResponse;

public interface RenewLeaseContractUseCase {
    LeaseContractRenewalResponse execute(RenewLeaseContractCommand command);
}
