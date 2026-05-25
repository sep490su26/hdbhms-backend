package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;

import java.util.List;
import java.util.Optional;

public interface LeaseContractRepository {
    LeaseContract save(LeaseContract leaseContract);

    Optional<LeaseContract> findById(Long id);

    List<LeaseContract> findAllByTenantPersonProfileId(Long tenantId);
}
