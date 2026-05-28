package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetLeaseContractDetailsQuery;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;

public interface GetLeaseContractDetailsUseCase {
    LeaseContract execute(GetLeaseContractDetailsQuery query);
}
