package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.RecordTenantIntentionCommand;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;

public interface RecordTenantIntentionUseCase {
    LeaseContractManagementResponse executeForManagement(RecordTenantIntentionCommand command);
    LeaseContractManagementResponse executeForCurrentUser(RecordTenantIntentionCommand command);
    LeaseContractManagementResponse executeForCurrentTenant(RecordTenantIntentionCommand command);
}
