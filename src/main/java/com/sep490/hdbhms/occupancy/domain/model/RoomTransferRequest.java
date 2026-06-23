package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.TargetTransferType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
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
    TargetTransferType targetTransferType;
    Long targetContractId;
    Long targetHolderApprovedById;
    LocalDateTime targetHolderApprovedAt;
    LocalDateTime targetHolderRejectedAt;

    LocalDate requestedTransferDate;
    String reason;
    Integer reservedSlots;
    LocalDateTime reservationExpiresAt;

    @Builder.Default
    TransferRequestStatus status = TransferRequestStatus.WAITING_APPROVAL;
    Long debtSnapshotId;
    Long newContractId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public boolean requiresHolderNomination(Long currentHolderProfileId) {
        return transferringTenantProfileIds != null
                && transferringTenantProfileIds.contains(currentHolderProfileId)
                && nominatedHolderProfileId == null;
    }
}
