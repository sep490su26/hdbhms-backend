package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    LocalDate requestedTransferDate;
    String reason;
    @Builder.Default
    TransferRequestStatus status = TransferRequestStatus.PENDING;
    Long debtSnapshotId;
    Long approvedById;
    LocalDateTime approvedAt;
    String rejectionReason;
    LocalDateTime eligibilityCheckedAt;
    Boolean isEligibleAtCreation;
    byte[] eligibilitySnapshot;
    Long newContractId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
