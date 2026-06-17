package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomTransferRequestRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.RoomTransferRequestPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataRoomTransferRepository implements RoomTransferRepository {
    JpaRoomTransferRequestRepository jpaRoomTransferRequestRepository;
    RoomTransferRequestPersistenceMapper roomTransferRequestPersistenceMapper;

    @Override
    public RoomTransferRequest save(RoomTransferRequest roomTransferRequest) {
        return roomTransferRequestPersistenceMapper.toDomain(
                jpaRoomTransferRequestRepository.save(
                        roomTransferRequestPersistenceMapper.toEntity(
                                roomTransferRequest
                        )
                )
        );
    }
}
