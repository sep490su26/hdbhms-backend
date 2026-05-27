package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetDepositAgreementDetailsQuery;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;

public interface GetDepositAgreementDetailsUseCase {
    DepositAgreement execute(GetDepositAgreementDetailsQuery query);
}
