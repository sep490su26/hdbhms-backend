package com.sep490.hdbhms.maintenance.domain.model;

import com.sep490.hdbhms.maintenance.domain.valueObjects.CostType;
import com.sep490.hdbhms.maintenance.domain.valueObjects.CostResponsibility;
import com.sep490.hdbhms.maintenance.domain.valueObjects.PaidBy;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceCost {
    Long id;
    Long ticketId;
    CostType costType;
    String description;
    Long amount;
    PaidBy paidBy;
    CostResponsibility costResponsibility;
    Long chargeInvoiceId;
    Long receiptFileId;
    Long createdById;
    LocalDateTime createdAt;
}
