package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.valueObjects.TransferRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomTransferRequestRepository {
    long sumActiveReservedSlotsByRoomId(Long id, List<TransferRequestStatus> activeReservationStatuses, LocalDateTime localDateTime, Long excludedTransferRequestId);
    boolean existsOpenByOldContractId(Long oldContractId, List<TransferRequestStatus> openStatuses);
}
