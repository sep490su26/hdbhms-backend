package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

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

    Long batchId;
    String batchStatus;
    List<RoomBatchStatus> rooms;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomBatchStatus {
        Long roomId;
        String roomCode;
        String roomName;
        
        BigDecimal electricityPrevious;
        BigDecimal electricityCurrent;
        BigDecimal waterPrevious;
        BigDecimal waterCurrent;
        
        String status; // e.g., "pending", "synced"
        LocalDateTime syncTime;
        Integer photosCount;
    }
}
