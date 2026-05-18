package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.FloorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaFloorRepository extends JpaRepository<FloorEntity, Long> {
}
