package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ContractHandoverDetailsResponse {

    Long handoverRecordId;
    HandoverType handoverType;
    HandoverStatus status;
    LocalDateTime handoverDate;
    String note;

    Long signedDocumentId;
    String signedDocumentUrl;

    MeterReadingDetails electricity;
    MeterReadingDetails water;

    @Builder.Default
    List<HandoverItemDetails> items = List.of();

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MeterReadingDetails {
        Long id;
        BigDecimal currentValue;
        LocalDateTime readingDate;
        Long photoFileId;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class HandoverItemDetails {
        Long id;
        String assetName;
        Integer quantity;
        AssetCondition conditionStatus;
        String note;
        Long evidenceFileId;
        String evidenceFileUrl;
    }
}
