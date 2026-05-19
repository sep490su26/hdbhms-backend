package com.sep490.hdbhms.maintenance.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;

import java.util.List;

public interface GetLeaseContractOfTenantPort {
    List<LeaseContract> execute(Long tenantId);
}
