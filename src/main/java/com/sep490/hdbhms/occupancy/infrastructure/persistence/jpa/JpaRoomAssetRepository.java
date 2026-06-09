package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaRoomAssetRepository extends JpaRepository<RoomAssetEntity, Long> {
    List<RoomAssetEntity> findByRoom_IdAndDeletedAtIsNull(Long roomId);
    Optional<RoomAssetEntity> findByIdAndRoom_IdAndDeletedAtIsNull(Long id, Long roomId);
}
