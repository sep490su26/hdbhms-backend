package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LatestMeterReadingsResponse {

    ReadingDetail electricity;
    ReadingDetail water;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReadingDetail {
        BigDecimal previousValue;
        BigDecimal suggestedValue;
        LocalDate lastReadingDate;
    }
}
