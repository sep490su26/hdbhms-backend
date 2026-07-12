package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.RoomTransferRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomTransferRepository {
    RoomTransferRequest save(RoomTransferRequest roomTransferRequest);
    Optional<RoomTransferRequest> findById(Long id);

    Optional<RoomTransferRequest> findByRequestCode(String requestCode);
    List<RoomTransferRequest> findByStatusAndUpdatedAtBefore(TransferRequestStatus status, LocalDateTime updatedBefore);
    List<RoomTransferRequest> findPendingHolderNominations(Long holderUserId);
    List<RoomTransferRequest> findPendingTargetHolderApprovals(Long holderUserId);
}
