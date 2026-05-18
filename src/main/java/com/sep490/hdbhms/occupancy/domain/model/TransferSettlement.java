package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransferSettlement {
    Long id;
    Long transferRequestId;
    @Builder.Default
    Long oldRoomRemainingValue = 0L;
    @Builder.Default
    Long newRoomRequiredValue = 0L;
    @Builder.Default
    Long differenceAmount = 0L;
    SettlementType settlementType;
    Long oldRoomFinalInvoiceId;
    Long transferDifferenceInvoiceId;
    Long confirmedById;
    LocalDateTime confirmedAt;
    LocalDateTime createdAt;
}
