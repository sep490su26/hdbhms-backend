package com.sep490.hdbhms.property.application.port.in.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmitSingleMeterReadingCommand(
        Long roomId,
        String readingPeriod,
        LocalDate readingDate,
        BigDecimal electricityValue,
        Long electricityPhotoId,
        Integer rolloverCount
) {
}
