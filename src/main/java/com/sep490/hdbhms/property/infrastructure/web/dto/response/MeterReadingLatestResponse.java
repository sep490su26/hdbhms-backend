package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import com.sep490.hdbhms.property.domain.value_objects.MeterType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MeterReadingLatestResponse(
        Item electricity
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
