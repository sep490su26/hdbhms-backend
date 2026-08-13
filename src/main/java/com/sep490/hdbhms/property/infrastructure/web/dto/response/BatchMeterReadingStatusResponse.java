package com.sep490.hdbhms.property.infrastructure.web.dto.response;

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
public class BatchMeterReadingStatusResponse {

    Long propertyId;
    String propertyName;
    String readingPeriod;
    Long batchId;
    String batchStatus;
    String billingRunStatus;
    boolean readingsLocked;
    UtilityTariffSnapshot electricityTariff;
    List<RoomBatchStatus> rooms;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UtilityTariffSnapshot {
        Long unitPrice;
        Long freeAllowance;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomBatchStatus {
        Long roomId;
        String roomCode;
        String roomName;
        
        BigDecimal electricityPrevious;
        BigDecimal electricityCurrent;
        Long electricityPhotoId;
        
        String status; // e.g., "pending", "synced"
        LocalDateTime syncTime;
        Integer photosCount;
        List<ReadingWarning> warnings;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReadingWarning {
        Long id;
        String meterType;
        String type;
        String severity;
        String message;
    }
}
