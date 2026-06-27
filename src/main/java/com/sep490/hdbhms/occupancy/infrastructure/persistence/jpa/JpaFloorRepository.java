package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.FloorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaFloorRepository extends JpaRepository<FloorEntity, Long> {
    List<FloorEntity> findAllByProperty_Id(Long propertyId);

    List<FloorEntity> findAllByProperty_IdAndDeletedAtIsNull(Long propertyId);

    boolean existsByProperty_IdAndFloorCodeAndDeletedAtIsNull(Long propertyId, String floorCode);
}
