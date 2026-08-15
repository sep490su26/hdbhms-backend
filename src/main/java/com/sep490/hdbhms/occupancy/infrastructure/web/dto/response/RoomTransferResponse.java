package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.TargetTransferType;
import com.sep490.hdbhms.occupancy.domain.value_objects.TransferRequestStatus;

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
    LeaseStatus oldContractStatus,
    Long oldRoomId,
    String oldRoomCode,
    String oldRoomName,
    Long oldRoomFloorId,
    Long targetRoomId,
    String targetRoomCode,
    String targetRoomName,
    Long targetRoomFloorId,
    String tenantName,
    String tenantPhone,
    List<Long> transferringTenantProfileIds,
    Map<Long, String> transferringTenantNames,
    List<Long> sourceHolderCandidateProfileIds,
    Map<Long, String> sourceHolderCandidateNames,
    Long nominatedHolderProfileId,
    TargetTransferType targetTransferType,
    Long targetContractId,
    LeaseStatus targetContractStatus,
    LocalDate requestedTransferDate,
    LocalDate expectedTransferDate,
    String reason,
    Integer reservedSlots,
    LocalDateTime reservationExpiresAt,
    Long targetHolderApprovedById,
    LocalDateTime targetHolderApprovedAt,
    LocalDateTime targetHolderRejectedAt,
    Long approvedById,
    String approvedByName,
    LocalDateTime approvedAt,
    LocalDateTime executedAt,
    LocalDateTime completedAt,
    LocalDateTime actualTransferDate,
    TransferRequestStatus status,
    Long newContractId,
    LeaseStatus newContractStatus,
    Long replacementOldContractId,
    LeaseStatus replacementOldContractStatus,
    Long oldRoomPrice,
    Long newRoomPrice,
    Long priceDifferenceAmount,
    Long priceDifferenceToPay,
    Boolean sourceRoomWillBeEmptyAfterTransfer,
    Integer remainingOccupantCountAfterTransfer,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    SettlementType priceDifferenceSettlementType,
    Long transferDifferenceInvoiceId,
    Long oldRoomFinalInvoiceId,
    DebtSummary debtSummary,
    ViolationSummary violationSummary,
    Integer transferCountThisYear,
    LocalDateTime eligibilityCheckedAt,
    Boolean eligibleAtCreation,
    String eligibilitySnapshot,
    String violationSnapshot,
    String transferHistorySnapshot,
    List<String> eligibilityWarnings,
    String paymentBranch,
    Boolean transferOutHandoverRequired,
    Boolean transferInHandoverRequired,
    Boolean roomHandoverRequired,
    List<String> allowedActions,
    List<String> blockingReasons
) {
    public record DebtSummary(
            Long rentDebtAmount,
            Long utilityDebtAmount,
            Long otherDebtAmount,
            Integer rentDebtMonths,
            Integer utilityDebtMonths,
            Long totalDebtAmount,
            Long debtLimitAmount,
            Boolean overLimit
    ) {}

    public record ViolationSummary(
            Integer totalCount,
            List<String> latestDescriptions
    ) {}

}
