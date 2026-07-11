package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MeterReadingLatestResponse(
        Item electricity,
        Item water
) {
    public record Item(
            Long id,
            MeterType meterType,
            BigDecimal currentIndex,
            BigDecimal currentValue,
            BigDecimal suggestedValue,
            String readingPeriod,
            LocalDate readingDate,
            LocalDate lastReadingDate,
            LocalDateTime recordedAt
    ) {
    }
}
