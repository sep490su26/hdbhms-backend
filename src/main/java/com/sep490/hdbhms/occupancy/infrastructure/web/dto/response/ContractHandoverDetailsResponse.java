package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MeterReadingDetails {
        Long id;
        BigDecimal currentValue;
        LocalDateTime readingDate;
        Long photoFileId;
    }
}
