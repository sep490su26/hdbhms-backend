package com.sep490.hdbhms.booking.infrastructure.persistence.jpa;

import com.sep490.hdbhms.booking.infrastructure.persistence.entity.RoomDepositFailureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaRoomDepositFailureRepository extends JpaRepository<RoomDepositFailureEntity, Long> {
    boolean existsByRoomHoldId(Long roomHoldId);

    List<RoomDepositFailureEntity> findByRoomIdAndOccurredAtAfterOrderByOccurredAtAsc(
            Long roomId,
            LocalDateTime occurredAfter
    );
}
