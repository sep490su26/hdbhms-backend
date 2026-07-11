package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.RoomHoldRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomHoldRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.RoomHoldPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataRoomHoldRepository implements RoomHoldRepository {
    JpaRoomHoldRepository jpaRoomHoldRepository;
    RoomHoldPersistenceMapper roomHoldPersistenceMapper;

    @Override
    public RoomHold save(RoomHold roomHold) {
        return roomHoldPersistenceMapper.toDomain(
                jpaRoomHoldRepository.save(
                        roomHoldPersistenceMapper.toEntity(roomHold)
                )
        );
    }

    @Override
    public Optional<RoomHold> findById(Long id) {
        return jpaRoomHoldRepository.findById(id)
                .map(roomHoldPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByRoomIdAndStatusIn(Long roomId, List<RoomHoldStatus> active) {
        return jpaRoomHoldRepository.existsByRoom_IdAndStatusIn(roomId, active);
    }

    @Override
    public List<RoomHold> findExpiredUnconfirmedHolds(LocalDateTime now) {
        return jpaRoomHoldRepository
                .findExpiredUnconfirmedHoldsWithRoomStatus(now, RoomHoldStatus.CONFIRMED, RoomStatus.ON_HOLD)
                .stream()
                .map(roomHoldPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<RoomHold> findActiveHoldByRoomId(Long roomId, LocalDateTime now) {
        return jpaRoomHoldRepository
                .findFirstByRoom_IdAndStatusInAndExpiresAtAfterOrderByExpiresAtAsc(
                        roomId,
                        List.of(RoomHoldStatus.ACTIVE, RoomHoldStatus.PAYMENT_PROCESSING),
                        now
                )
                .map(roomHoldPersistenceMapper::toDomain);
    }
}
