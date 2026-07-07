package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.RoomPersistenceMapper;
import com.sep490.hdbhms.shared.specifications.RoomSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataRoomRepository implements RoomRepository {
    JpaRoomRepository jpaRoomRepository;
    RoomPersistenceMapper roomPersistenceMapper;

    @Override
    public Room save(Room room) {
        return roomPersistenceMapper.toDomain(
                jpaRoomRepository.save(
                        roomPersistenceMapper.toEntity(room)
                )
        );
    }

    @Override
    public Optional<Room> findById(Long id) {
        return jpaRoomRepository.findById(id)
                .map(roomPersistenceMapper::toDomain);
    }

    @Override
    public List<Room> findAllByPropertyIdAndFloorId(
            Long propertyId,
            Long floorId
    ) {
        if (floorId == null) {
            return jpaRoomRepository.findAllByProperty_IdAndDeletedAtIsNull(propertyId).stream()
                    .map(roomPersistenceMapper::toDomain)
                    .toList();
        }
        return jpaRoomRepository
                .findAllByProperty_IdAndFloor_IdAndDeletedAtIsNull(propertyId, floorId).stream()
                .map(roomPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Page<Room> findAll(
            List<Long> ids,
            RoomStatus status,
            Long minPrice,
            Long maxPrice,
            Pageable pageable
    ) {
        Specification<RoomEntity> specification = Specification
                .where(RoomSpecifications.idIn(ids))
                .and(RoomSpecifications.notDeleted())
                .and(RoomSpecifications.statusIn(status))
                .and(RoomSpecifications.priceBetween(minPrice, maxPrice));
        return jpaRoomRepository.findAll(specification, pageable)
                .map(roomPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Room> findByRoomCode(String roomCode) {
        return jpaRoomRepository.findByRoomCode(roomCode)
                .map(roomPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsActiveByPropertyIdAndRoomCode(Long propertyId, String roomCode) {
        return jpaRoomRepository.existsByProperty_IdAndRoomCodeAndDeletedAtIsNull(propertyId, roomCode);
    }

    @Override
    public int updateRoomStatusIfCurrent(Long roomId, RoomStatus expectedStatus, RoomStatus newStatus) {
        return jpaRoomRepository.updateRoomStatusIfCurrent(
                roomId,
                expectedStatus,
                newStatus
        );
    }

    @Override
    public List<Room> findAll() {
        return jpaRoomRepository.findAll().stream()
                .map(roomPersistenceMapper::toDomain)
                .toList();
    }
}
