package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomHoldEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaRoomHoldRepository extends JpaRepository<RoomHoldEntity, Long> {
    boolean existsByRoom_IdAndStatusIn(Long roomId, List<RoomHoldStatus> active);
}
