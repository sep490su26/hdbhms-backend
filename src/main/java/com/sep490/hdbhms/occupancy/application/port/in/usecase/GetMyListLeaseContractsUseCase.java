package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListLeaseContractsQuery;
import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import org.springframework.data.domain.Page;

public interface GetMyListLeaseContractsUseCase {
    Page<LeaseContract> execute(GetListLeaseContractsQuery query);
}
