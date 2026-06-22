package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomTransferRequest {
    Long id;
    String requestCode;
    Long requesterId;
    Long oldContractId;
    Long oldRoomId;
    Long targetRoomId;
    List<Long> transferringTenantProfileIds;
    Long nominatedHolderProfileId;
    String targetTransferType; // NEW_CONTRACT, OWN_CONTRACT, OTHER_CONTRACT
    LocalDate requestedTransferDate;
    String reason;
    @Builder.Default
    TransferRequestStatus status = TransferRequestStatus.OLD_ROOM_HANDOVER;
    Long debtSnapshotId;
    Long newContractId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
