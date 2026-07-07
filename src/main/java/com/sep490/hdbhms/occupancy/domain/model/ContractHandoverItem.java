package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.AssetCondition;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractHandoverItem {
    Long id;
    Long handoverRecordId;
    Long roomAssetId;
    String assetName;
    @Builder.Default
    Integer quantity = 1;
    @Builder.Default
    AssetCondition conditionStatus = AssetCondition.GOOD;
    String note;
    Long evidenceFileId;
    Long compensationAmount;
    Long compensationInvoiceId;
    LocalDateTime createdAt;
}
