package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListDepositAgreementsQuery;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;
import org.springframework.data.domain.Page;

public interface GetMyListDepositAgreementsUseCase {
    Page<DepositAgreement> execute(GetListDepositAgreementsQuery query);
}
