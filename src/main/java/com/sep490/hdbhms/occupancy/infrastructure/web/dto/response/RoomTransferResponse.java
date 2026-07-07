package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.SettlementType;
import com.sep490.hdbhms.occupancy.domain.valueObjects.TargetTransferType;
import com.sep490.hdbhms.occupancy.domain.valueObjects.TransferRequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RoomTransferResponse(
    Long id,
    String requestCode,
    Long requesterId,
    Long oldContractId,
    String oldContractCode,
    Long oldRoomId,
    String oldRoomCode,
    String oldRoomName,
    Long targetRoomId,
    String targetRoomCode,
    String targetRoomName,
    List<Long> transferringTenantProfileIds,
    Map<Long, String> transferringTenantNames,
    List<Long> sourceHolderCandidateProfileIds,
    Map<Long, String> sourceHolderCandidateNames,
    Long nominatedHolderProfileId,
    TargetTransferType targetTransferType,
    Long targetContractId,
    LocalDate requestedTransferDate,
    LocalDate expectedTransferDate,
    String reason,
    Integer reservedSlots,
    LocalDateTime reservationExpiresAt,
    Long targetHolderApprovedById,
    LocalDateTime targetHolderApprovedAt,
    LocalDateTime targetHolderRejectedAt,
    TransferRequestStatus status,
    Long newContractId,
    Long replacementOldContractId,
    Long oldRoomPrice,
    Long newRoomPrice,
    Long priceDifferenceToPay,
    Boolean sourceRoomWillBeEmptyAfterTransfer,
    Integer remainingOccupantCountAfterTransfer,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    SettlementType priceDifferenceSettlementType,
    Long transferDifferenceInvoiceId,
    Long oldRoomFinalInvoiceId,
    String paymentBranch,
    Boolean transferOutHandoverRequired,
    Boolean transferInHandoverRequired,
    Boolean roomHandoverRequired,
    List<String> allowedActions,
    List<String> blockingReasons
) {}
