package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SubmitBatchMeterReadingsCommand(
        Long propertyId,
        String readingPeriod,
        LocalDate readingDate,
        List<RoomReading> readings
) {
    public record RoomReading(
            Long roomId,
            BigDecimal electricityValue,
            BigDecimal waterValue,
            Long electricityPhotoId,
            Long waterPhotoId
    ) {
    }
}
