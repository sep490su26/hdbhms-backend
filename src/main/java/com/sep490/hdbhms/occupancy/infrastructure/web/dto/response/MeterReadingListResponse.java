package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterType;
import com.sep490.hdbhms.occupancy.domain.valueObjects.ReadingSource;
import com.sep490.hdbhms.occupancy.domain.valueObjects.ReadingStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeterReadingListResponse {

    List<RoomReadingGroup> rooms;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoomReadingGroup {
        Long roomId;
        String roomCode;
        String roomName;
        Long propertyId;
        String propertyName;
        List<MeterReadingEntry> readings;
    }

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class MeterReadingEntry {
        Long id;
        MeterType meterType;
        String readingPeriod;
        BigDecimal previousValue;
        BigDecimal currentValue;
        BigDecimal usageAmount;
        LocalDate readingDate;
        ReadingSource source;
        ReadingStatus status;
        Long photoFileId;
        String createdByName;
        LocalDateTime createdAt;
    }
}
