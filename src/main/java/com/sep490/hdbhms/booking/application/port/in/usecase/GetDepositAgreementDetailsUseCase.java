package com.sep490.hdbhms.booking.application.port.in.usecase;

import com.sep490.hdbhms.booking.application.port.in.query.GetDepositAgreementDetailsQuery;
import com.sep490.hdbhms.booking.domain.model.DepositAgreement;

public interface GetDepositAgreementDetailsUseCase {
    DepositAgreement execute(GetDepositAgreementDetailsQuery query);
}
