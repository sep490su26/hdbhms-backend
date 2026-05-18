package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRoomAssetRepository extends JpaRepository<RoomAssetEntity, Long> {
}
