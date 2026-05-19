package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.model.LeaseContract;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaLeaseContractRepository extends JpaRepository<LeaseContractEntity, Long> {
    List<LeaseContractEntity> findAllByPrimaryTenantProfile_Id(Long tenantPersonProfileId);
}
