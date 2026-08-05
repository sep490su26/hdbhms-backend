package com.sep490.hdbhms.booking.application.port.in.usecase;

import com.sep490.hdbhms.booking.application.port.in.query.GetDepositFormsQuery;

public interface GetDepositFormsUseCase {
    void execute(GetDepositFormsQuery query);
}
