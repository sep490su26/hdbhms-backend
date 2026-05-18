package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomTransferRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaRoomTransferRequestRepository extends JpaRepository<RoomTransferRequestEntity, Long> {
}
