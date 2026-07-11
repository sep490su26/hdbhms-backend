package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.LiquidationStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractLiquidation {
    Long id;
    Long contractId;
    LocalDate liquidationDate;
    String reason;
    @Builder.Default
    Long depositAmount = 0L;
    @Builder.Default
    Long depositDeductionAmount = 0L;
    String depositDeductionReason;
    @Builder.Default
    Long depositRefundAmount = 0L;
    Long finalInvoiceId;
    Long signedFileId;
    @Builder.Default
    LiquidationStatus status = LiquidationStatus.DRAFT;
    Long createdById;
    LocalDateTime createdAt;
}
