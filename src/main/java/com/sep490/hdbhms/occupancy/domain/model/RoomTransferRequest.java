package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
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
    TransferRequestStatus status = TransferRequestStatus.REQUESTED;
    Long debtSnapshotId;
    Long newContractId; // Destination contract for NEW_CONTRACT, transfer agreement for OTHER_CONTRACT.
    Long replacementOldContractId;
    SettlementType positiveDifferenceSettlementType; // Choice made at tenant confirmation
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public boolean requiresHolderNomination(Long currentHolderProfileId) {
        return transferringTenantProfileIds != null
                && transferringTenantProfileIds.contains(currentHolderProfileId)
                && nominatedHolderProfileId == null;
    }
}
