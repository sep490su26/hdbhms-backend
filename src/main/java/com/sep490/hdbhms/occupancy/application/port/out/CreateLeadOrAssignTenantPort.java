package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;

public interface CreateLeadOrAssignTenantPort {
    void execute(DepositAgreement depositAgreement);
}
