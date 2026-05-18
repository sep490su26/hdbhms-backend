package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeaseContractEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaLeaseContractRepository extends JpaRepository<LeaseContractEntity, Long> {
}
