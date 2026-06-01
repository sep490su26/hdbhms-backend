package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRoomImageRepository extends JpaRepository<RoomImageEntity, Long> {
    List<RoomImageEntity> findAllByRoom_Id(Long roomId);
}
