package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.HandoverType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractHandoverRecord {
    Long id;
    Long contractId;
    Long roomId;
    HandoverType handoverType;
    LocalDateTime handoverDate;
    Long electricityReadingId;
    Long waterReadingId;
    String note;
    @Builder.Default
    HandoverStatus status = HandoverStatus.DRAFT;
    Long confirmedById;
    LocalDateTime confirmedAt;
    LocalDateTime createdAt;
}
