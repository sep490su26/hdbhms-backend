package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.BatchStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeterReadingBatchHistoryResponse {

    List<BatchHistoryItem> history;

    @Getter
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BatchHistoryItem {
        Long batchId;
        String period;
        Boolean isCurrent;
        LocalDate startDate;
        LocalDate endDate;
        BatchStatus status;
        Integer totalRooms;
        Integer completedRooms;
        Integer anomalyCount;
    }
}
