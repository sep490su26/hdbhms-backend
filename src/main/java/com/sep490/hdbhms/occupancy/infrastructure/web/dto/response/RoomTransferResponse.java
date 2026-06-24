package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.TargetTransferType;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RoomTransferResponse(
    Long id,
    String requestCode,
    Long requesterId,
    Long oldContractId,
    Long oldRoomId,
    Long targetRoomId,
    List<Long> transferringTenantProfileIds,
    Long nominatedHolderProfileId,
    TargetTransferType targetTransferType,
    Long targetContractId,
    LocalDate requestedTransferDate,
    String reason,
    Integer reservedSlots,
    LocalDateTime reservationExpiresAt,
    Long targetHolderApprovedById,
    LocalDateTime targetHolderApprovedAt,
    LocalDateTime targetHolderRejectedAt,
    TransferRequestStatus status,
    Long newContractId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
