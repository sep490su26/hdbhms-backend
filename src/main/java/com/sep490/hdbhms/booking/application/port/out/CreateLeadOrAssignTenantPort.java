package com.sep490.hdbhms.booking.application.port.out;

import com.sep490.hdbhms.booking.domain.model.DepositAgreement;

public interface CreateLeadOrAssignTenantPort {
    void execute(DepositAgreement depositAgreement);
}
