package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.RoomTransferRequestRepository;
import com.sep490.hdbhms.occupancy.domain.valueObjects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomTransferRequestRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataRoomTransferRequestRepository implements RoomTransferRequestRepository {
    JpaRoomTransferRequestRepository jpaRoomTransferRequestRepository;
    @Override
    public long sumActiveReservedSlotsByRoomId(Long id, List<TransferRequestStatus> activeReservationStatuses, LocalDateTime localDateTime, Long excludedTransferRequestId) {
        return jpaRoomTransferRequestRepository.sumActiveReservedSlotsByRoomId(id, activeReservationStatuses, localDateTime, excludedTransferRequestId);
    }

    @Override
    public boolean existsOpenByOldContractId(Long oldContractId, List<TransferRequestStatus> openStatuses) {
        return jpaRoomTransferRequestRepository.existsByOldContract_IdAndStatusIn(oldContractId, openStatuses);
    }
}
