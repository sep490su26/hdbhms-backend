package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.VisitRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaVisitRequestRepository extends JpaRepository<VisitRequestEntity, Long>, JpaSpecificationExecutor<VisitRequestEntity> {
}
