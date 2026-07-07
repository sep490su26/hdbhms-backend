package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.valueObjects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomTransferRequestRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.RoomTransferRequestPersistenceMapper;
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

    @Override
    public Optional<RoomTransferRequest> findById(Long id) {
        return jpaRoomTransferRequestRepository.findById(id)
                .map(roomTransferRequestPersistenceMapper::toDomain);
    }

    @Override
    public Optional<RoomTransferRequest> findByRequestCode(String requestCode) {
        return jpaRoomTransferRequestRepository.findByRequestCode(requestCode)
                .map(roomTransferRequestPersistenceMapper::toDomain);
    }

    @Override
    public List<RoomTransferRequest> findByStatusAndUpdatedAtBefore(TransferRequestStatus status, LocalDateTime updatedBefore) {
        return jpaRoomTransferRequestRepository.findByStatusAndUpdatedAtBefore(status, updatedBefore)
                .stream()
                .map(roomTransferRequestPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<RoomTransferRequest> findPendingTargetHolderApprovals(Long holderUserId) {
        return jpaRoomTransferRequestRepository.findPendingTargetHolderApprovals(
                        TransferRequestStatus.WAITING_TARGET_HOLDER_APPROVAL, holderUserId)
                .stream()
                .map(roomTransferRequestPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<RoomTransferRequest> findPendingHolderNominations(Long holderUserId) {
        return jpaRoomTransferRequestRepository.findPendingHolderNominations(
                        TransferRequestStatus.WAITING_HOLDER_RESPONSE, holderUserId)
                .stream()
                .map(roomTransferRequestPersistenceMapper::toDomain)
                .toList();
    }
}
