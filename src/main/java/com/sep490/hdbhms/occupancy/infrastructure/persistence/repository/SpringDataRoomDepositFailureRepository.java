package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.RoomDepositFailureRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomDepositFailure;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomDepositFailureRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.RoomDepositFailurePersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataRoomDepositFailureRepository implements RoomDepositFailureRepository {
    JpaRoomDepositFailureRepository jpaRoomDepositFailureRepository;
    RoomDepositFailurePersistenceMapper roomDepositFailurePersistenceMapper;

    @Override
    public RoomDepositFailure save(RoomDepositFailure failure) {
        return roomDepositFailurePersistenceMapper.toDomain(
                jpaRoomDepositFailureRepository.save(
                        roomDepositFailurePersistenceMapper.toEntity(failure)
                )
        );
    }

    @Override
    public boolean existsByRoomHoldId(Long roomHoldId) {
        return roomHoldId != null && jpaRoomDepositFailureRepository.existsByRoomHoldId(roomHoldId);
    }

    @Override
    public List<RoomDepositFailure> findByRoomIdAndOccurredAtAfter(Long roomId, LocalDateTime occurredAfter) {
        return jpaRoomDepositFailureRepository
                .findByRoomIdAndOccurredAtAfterOrderByOccurredAtAsc(roomId, occurredAfter)
                .stream()
                .map(roomDepositFailurePersistenceMapper::toDomain)
                .toList();
    }
}
