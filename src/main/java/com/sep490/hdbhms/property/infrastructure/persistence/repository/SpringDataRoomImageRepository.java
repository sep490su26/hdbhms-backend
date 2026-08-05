package com.sep490.hdbhms.property.infrastructure.persistence.repository;

import com.sep490.hdbhms.property.application.port.out.RoomImageRepository;
import com.sep490.hdbhms.property.domain.model.RoomImage;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomImageRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.mapper.RoomImagePersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataRoomImageRepository implements RoomImageRepository {
    JpaRoomImageRepository jpaRoomImageRepository;
    RoomImagePersistenceMapper roomImagePersistenceMapper;

    @Override
    public RoomImage save(RoomImage roomImage) {
        return roomImagePersistenceMapper.toDomain(
                jpaRoomImageRepository.save(
                        roomImagePersistenceMapper.toEntity(
                                roomImage
                        )
                )
        );
    }

    @Override
    public Optional<RoomImage> findById(Long id) {
        return jpaRoomImageRepository.findById(id)
                .map(roomImagePersistenceMapper::toDomain);
    }

    @Override
    public List<RoomImage> findAllByRoomId(Long roomId) {
        return jpaRoomImageRepository.findAllByRoom_IdOrderBySortOrderAscCreatedAtAscIdAsc(roomId).stream()
                .map(roomImagePersistenceMapper::toDomain)
                .toList();
    }
}
