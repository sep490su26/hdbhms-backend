package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomHoldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaRoomHoldRepository extends JpaRepository<RoomHoldEntity, Long> {
    boolean existsByRoom_IdAndStatusIn(Long roomId, List<RoomHoldStatus> active);

    Optional<RoomHoldEntity> findFirstByRoom_IdAndStatusInAndExpiresAtAfterOrderByExpiresAtAsc(
            Long roomId,
            List<RoomHoldStatus> statuses,
            LocalDateTime now
    );

    @Query("""
            SELECT h FROM RoomHoldEntity h
            WHERE h.expiresAt < :now
              AND h.status <> :confirmedStatus
              AND h.room.currentStatus = :roomStatus
            """)
    List<RoomHoldEntity> findExpiredUnconfirmedHoldsWithRoomStatus(
            @Param("now") LocalDateTime now,
            @Param("confirmedStatus") RoomHoldStatus confirmedStatus,
            @Param("roomStatus") RoomStatus roomStatus
    );
}
