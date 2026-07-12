package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.FloorPlanItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaFloorPlanItemRepository extends JpaRepository<FloorPlanItemEntity, Long> {
    List<FloorPlanItemEntity> findAllByProperty_IdAndFloor_IdOrderByIdAsc(Long propertyId, Long floorId);

    List<FloorPlanItemEntity> findAllByProperty_IdOrderByFloor_SortOrderAscIdAsc(Long propertyId);

    void deleteByProperty_IdAndFloor_Id(Long propertyId, Long floorId);

    void deleteByProperty_IdAndRoom_Id(Long propertyId, Long roomId);
}
