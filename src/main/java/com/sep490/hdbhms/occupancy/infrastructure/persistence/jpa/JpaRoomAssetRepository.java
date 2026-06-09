package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaRoomAssetRepository extends JpaRepository<RoomAssetEntity, Long> {
    @Query("""
            SELECT asset FROM RoomAssetEntity asset
            LEFT JOIN FETCH asset.imageFile
            WHERE asset.room.id = :roomId
              AND asset.deletedAt IS NULL
            ORDER BY asset.id ASC
            """)
    List<RoomAssetEntity> findActiveByRoomId(@Param("roomId") Long roomId);

    @Query("""
            SELECT asset FROM RoomAssetEntity asset
            LEFT JOIN FETCH asset.imageFile
            WHERE asset.id = :assetId
              AND asset.room.id = :roomId
              AND asset.deletedAt IS NULL
            """)
    Optional<RoomAssetEntity> findActiveByRoomIdAndId(
            @Param("roomId") Long roomId,
            @Param("assetId") Long assetId
    );
}
