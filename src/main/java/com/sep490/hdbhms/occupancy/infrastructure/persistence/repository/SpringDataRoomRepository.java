package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.RoomPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

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
}
