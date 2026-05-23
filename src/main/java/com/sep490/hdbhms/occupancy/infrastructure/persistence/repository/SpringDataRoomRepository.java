package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
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
        return jpaRoomRepository
                .findAllByProperty_IdAndFloor_Id(propertyId, floorId).stream()
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
                .and(RoomSpecifications.statusIn(status))
                .and(RoomSpecifications.priceBetween(minPrice, maxPrice));
        return jpaRoomRepository.findAll(specification, pageable)
                .map(roomPersistenceMapper::toDomain);
    }
}
