package com.sep490.hdbhms.property.infrastructure.persistence.jpa;

import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRoomImageRepository extends JpaRepository<RoomImageEntity, Long> {
    List<RoomImageEntity> findAllByRoom_IdOrderBySortOrderAscCreatedAtAscIdAsc(Long roomId);

    List<RoomImageEntity> findAllByRoom_IdInOrderByRoom_IdAscSortOrderAscCreatedAtAscIdAsc(List<Long> roomIds);
}
